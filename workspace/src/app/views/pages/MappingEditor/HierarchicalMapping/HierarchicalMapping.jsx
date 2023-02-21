import React from "react";
import _ from "lodash";
import { Spinner } from "@eccenca/gui-elements";
import PropTypes from "prop-types";

import { ruleRemoveAsync, setApiDetails } from "./store";

import MappingsTree from "./containers/MappingsTree";
import MappingsWorkview from "./containers/MappingsWorkview";
import MessageHandler from "./components/MessageHandler";
import { MAPPING_RULE_TYPE_OBJECT, MESSAGES } from "./utils/constants";
import RemoveMappingRuleDialog from "./elements/RemoveMappingRuleDialog";
import EventEmitter from "./utils/EventEmitter";
import { withHistoryHOC } from "./utils/withHistoryHOC";
import MappingEditorModal from "./MappingEditorModal";
import { getHistory } from "../../../../store/configureStore";
import PromptModal from "../../../../views/shared/projectTaskTabView/PromptModal";
import {requestValueTypes} from "./HierarchicalMapping.requests";
import {GlobalMappingEditorContext} from "../contexts/GlobalMappingEditorContext";

class HierarchicalMapping extends React.Component {
    // define property types
    static propTypes = {
        project: PropTypes.string.isRequired, // Current DI Project
        transformTask: PropTypes.string.isRequired, // Current Transformation
        initialRule: PropTypes.string,
        history: PropTypes.object,
        startFullScreen: PropTypes.bool,
        viewActions: PropTypes.object,
    };

    constructor(props) {
        super(props);
        const { initialRule, project, transformTask } = this.props;

        setApiDetails({
            project,
            transformTask,
        });

        this.state = {
            // currently selected rule id
            currentRuleId: _.isEmpty(initialRule) ? undefined : initialRule,
            // show / hide navigation
            showNavigation: true,
            // which edit view are we viewing
            elementToDelete: {},
            editingElements: [],
            askForDiscard: false,
            askForRemove: false,
            removeFunction: this.handleConfirmRemove,
            showMappingEditor: false,
            mappingEditorRuleId: undefined,
            valueTypeLabels: new Map()
        };
    }

    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        this.loadValueTypeLabels()
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevState.currentRuleId !== this.state.currentRuleId && !_.isEmpty(this.state.currentRuleId)) {
            const history = getHistory();
            const ruleId = this.state.currentRuleId;
            if (!this.props.startFullScreen) {
                history.replace({
                    search: `?${new URLSearchParams({ ruleId })}`,
                });
            }
        }
        if (prevProps.task !== this.props.task) {
            this.loadNavigationTree();
        }
    }

    async loadValueTypeLabels() {
        const valueTypes = (await requestValueTypes()).data
        const valueTypeMap = valueTypes.map(vt => [vt.value, vt.label ?? vt.value])
        this.setState({
            valueTypeLabels: new Map(valueTypeMap)
        })
    }

    onOpenEdit = (obj) => {
        const id = _.get(obj, "id", 0);
        if (!_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.concat(this.state.editingElements, [id]),
            });
        }
    };

    onCloseEdit = (obj) => {
        const id = _.get(obj, "id", 0);
        if (_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.filter(this.state.editingElements, (e) => e !== id),
            });
        }
    };

    handleClickRemove = (args, removeFn) => {
        /**
         * This scenario is default for most of cases
         * FIXME: move this functionality to RemoveConfirmDialog component and refactor this component which will work as a portal
         */
        if (args) {
            const { id, uri, type, parent } = args;
            this.setState({
                editingElements: [],
                elementToDelete: {
                    id,
                    uri,
                    type,
                    parent,
                },
                askForRemove: true,
                removeFunction: this.handleConfirmRemove,
            });
        } else if (_.isFunction(removeFn)) {
            // This scenario is for ObjectMappingRule, when we want to remove URI from complex
            const removeFunction = () => {
                removeFn();
                this.setState({
                    askForRemove: false,
                });
            };

            this.setState({
                askForRemove: true,
                removeFunction,
            });
        }
    };

    handleConfirmRemove = (event) => {
        event.stopPropagation();
        const { parent, type } = this.state.elementToDelete;
        this.setState({
            loading: true,
        });
        ruleRemoveAsync(this.state.elementToDelete.id).subscribe(
            () => {
                // FIXME: let know the user which element is gone!
                if (type === MAPPING_RULE_TYPE_OBJECT) {
                    this.setState({
                        currentRuleId: parent,
                        elementToDelete: {},
                        askForRemove: false,
                        loading: false,
                    });
                } else {
                    this.setState({
                        elementToDelete: {},
                        askForRemove: false,
                        loading: false,
                    });
                }
            },
            (err) => {
                // FIXME: let know the user what have happened!
                this.setState({
                    elementToDelete: {},
                    askForRemove: false,
                    loading: false,
                });
            }
        );
    };

    handleCancelRemove = () => {
        this.setState({
            elementToDelete: {},
            askForRemove: false,
        });
    };

    toggleAskForDiscard = (boolOrData) => {
        this.setState({
            askForDiscard: boolOrData,
        });
    };

    // react to rule id changes
    onRuleNavigation = ({ newRuleId }) => {
        console.log({
            newRuleId,
            currentRuleId: this.state.currentRuleId,
            editingElements: this.state.editingElements,
        });
        if (newRuleId === this.state.currentRuleId) {
            // Do nothing!
        } else if (this.state.editingElements.length === 0) {
            this.setState({
                currentRuleId: newRuleId,
            });
        } else {
            this.toggleAskForDiscard(newRuleId);
        }
    };

    // show / hide navigation
    handleToggleNavigation = (stateVisibility = !this.state.showNavigation) => {
        this.setState({
            showNavigation: stateVisibility,
        });
    };

    handleDiscardChanges = () => {
        if (_.includes(this.state.editingElements, 0)) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id: 0 });
        }
        this.setState({
            editingElements: [],
            currentRuleId: this.state.askForDiscard,
        });
        this.toggleAskForDiscard(false);
        this.props.viewActions.savedChanges && this.props.viewActions.savedChanges(false);
        EventEmitter.emit(MESSAGES.RULE_VIEW.DISCARD_ALL);
    };

    handleOpenMappingEditorModal = (mappingEditorRuleId) => {
        this.setState({ showMappingEditor: true, mappingEditorRuleId });
    };

    handleRuleIdChange = (rule) => {
        this.onRuleNavigation(rule);
    };

    // template rendering
    render() {
        const { currentRuleId, showMappingEditor, showNavigation, askForRemove, elementToDelete, askForDiscard } =
            this.state;
        const loading = this.state.loading ? <Spinner position={"global"} /> : false;

        // render mapping edit / create view of value and object
        return <GlobalMappingEditorContext.Provider
            value={{
                valueTypeLabels: this.state.valueTypeLabels
            }}
        >
            <section className="ecc-silk-mapping" data-test-id={"hierarchical-mappings"}>
                {showMappingEditor && this.state.mappingEditorRuleId ? (
                    <MappingEditorModal
                        projectId={this.props.project}
                        transformTaskId={this.props.transformTask}
                        ruleId={this.state.mappingEditorRuleId}
                        isOpen={showMappingEditor}
                        onClose={() => {
                            this.setState({
                                showMappingEditor: false,
                                mappingEditorRuleId: undefined,
                            });
                            // this.onRuleNavigation({ newRuleId: this.state.mappingEditorRuleId });
                            EventEmitter.emit(MESSAGES.RELOAD, true);
                        }}
                    />
                ) : null}
                {askForRemove && (
                    <RemoveMappingRuleDialog
                        mappingType={elementToDelete.type}
                        handleConfirmRemove={this.state.removeFunction}
                        handleCancelRemove={this.handleCancelRemove}
                    />
                )}
                <PromptModal
                    onClose={() => this.toggleAskForDiscard(false)}
                    proceed={this.handleDiscardChanges}
                    isOpen={askForDiscard}
                />
                {loading}
                <div className="ecc-temp__appmessages">
                    <MessageHandler />
                </div>
                <div className="ecc-silk-mapping__content">
                    {showNavigation && (
                        <MappingsTree
                            currentRuleId={currentRuleId}
                            handleRuleNavigation={this.onRuleNavigation}
                            startFullScreen={this.props.startFullScreen}
                        />
                    )}
                    {
                        <MappingsWorkview
                            currentRuleId={this.state.currentRuleId}
                            showNavigation={showNavigation}
                            onToggleTreeNav={this.handleToggleNavigation}
                            onRuleIdChange={this.handleRuleIdChange}
                            askForDiscardData={this.state.askForDiscard}
                            onAskDiscardChanges={this.toggleAskForDiscard}
                            onClickedRemove={this.handleClickRemove}
                            openMappingEditor={this.handleOpenMappingEditorModal}
                            startFullScreen={this.props.startFullScreen}
                            viewActions={this.props.viewActions}
                        />
                    }
                </div>
            </section>
        </GlobalMappingEditorContext.Provider>;
    }
}

export default withHistoryHOC(HierarchicalMapping);
