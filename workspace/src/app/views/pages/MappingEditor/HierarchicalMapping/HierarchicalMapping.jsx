import React from 'react';
import _ from 'lodash';
import {Spinner,} from 'gui-elements-deprecated';
import {URI} from 'ecc-utils';
import PropTypes from 'prop-types';

import {ruleRemoveAsync, setApiDetails} from './store';

import MappingsTree from './containers/MappingsTree';
import MappingsWorkview from './containers/MappingsWorkview';
import MessageHandler from './components/MessageHandler';
import {MAPPING_RULE_TYPE_OBJECT, MESSAGES} from './utils/constants';
import RemoveMappingRuleDialog from './elements/RemoveMappingRuleDialog';
import DiscardChangesDialog from './elements/DiscardChangesDialog';
import EventEmitter from './utils/EventEmitter';
import {withHistoryHOC} from "./utils/withHistoryHOC";

require('./HierarchicalMapping.scss');

class HierarchicalMapping extends React.Component {
    // define property types
    static propTypes = {
        project: PropTypes.string.isRequired, // Current DI Project
        transformTask: PropTypes.string.isRequired, // Current Transformation
        initialRule: PropTypes.string,
        history: PropTypes.object
    };

    constructor(props) {
        super(props);
        const {
            initialRule,
            project,
            transformTask
        } = this.props;

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
        };
    }

    componentDidMount() {
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.off(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
    }

    componentDidUpdate(prevProps, prevState) {
        if (
            prevState.currentRuleId !== this.state.currentRuleId &&
            !_.isEmpty(this.state.currentRuleId)
        ) {
            const href = window.location.href;
            try {
                const uriTemplate = new URI(href);
                const updatedUrl = HierarchicalMapping.updateMappingEditorUrl(uriTemplate, this.state.currentRuleId);
                this.props.history.pushState(null, '', updatedUrl);
            } catch (e) {
                console.debug(`HierarchicalMapping: ${href} is not an URI, cannot update the window state`);
            }
        }
        if (prevProps.task !== this.props.task) {
            this.loadNavigationTree();
        }
    }

    onOpenEdit = obj => {
        const id = _.get(obj, 'id', 0);
        if (!_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.concat(this.state.editingElements, [id]),
            });
        }
    };

    onCloseEdit = obj => {
        const id = _.get(obj, 'id', 0);
        if (_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.filter(
                    this.state.editingElements,
                    e => e !== id
                ),
            });
        }
    };

    handleClickRemove = (args, removeFn) => {
        /**
         * This scenario is default for most of cases
         * FIXME: move this functionality to RemoveConfirmDialog component and refactor this component which will work as a portal
         */
        if (args) {
            const {
                id, uri, type, parent,
            } = args;
            this.setState({
                editingElements: [],
                elementToDelete: {
                    id, uri, type, parent,
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

    handleConfirmRemove = event => {
        event.stopPropagation();
        const { parent, type } = this.state.elementToDelete;
        this.setState({
            loading: true,
        });
        ruleRemoveAsync(this.state.elementToDelete.id)
            .subscribe(
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
                err => {
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

    toggleAskForDiscard = boolOrData => {
        this.setState({
            askForDiscard: boolOrData,
        });
    };

    // react to rule id changes
    onRuleNavigation = ({ newRuleId }) => {
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
        EventEmitter.emit(MESSAGES.RULE_VIEW.DISCARD_ALL);
    };

    handleRuleIdChange = rule => {
        this.onRuleNavigation(rule);
    };

    // template rendering
    render() {
        const {
            currentRuleId, showNavigation, askForRemove,
            elementToDelete, askForDiscard, editingElements,
        } = this.state;
        const loading = this.state.loading ? <Spinner /> : false;

        // render mapping edit / create view of value and object
        return (
            <section className="ecc-silk-mapping">
                {
                    askForRemove && (
                        <RemoveMappingRuleDialog
                            mappingType={elementToDelete.type}
                            handleConfirmRemove={this.state.removeFunction}
                            handleCancelRemove={this.handleCancelRemove}
                        />
                    )
                }
                {
                    askForDiscard && (
                        <DiscardChangesDialog
                            handleDiscardConfirm={this.handleDiscardChanges}
                            handleDiscardCancel={() => this.toggleAskForDiscard(false)}
                            numberEditingElements={editingElements.length}
                        />
                    )
                }
                {loading}
                <div className="ecc-temp__appmessages">
                    <MessageHandler />
                </div>
                <div className="ecc-silk-mapping__content">
                    {
                        showNavigation && (
                            <MappingsTree
                                currentRuleId={currentRuleId}
                                handleRuleNavigation={this.onRuleNavigation}
                            />
                        )
                    }
                    {
                        <MappingsWorkview
                            currentRuleId={this.state.currentRuleId}
                            showNavigation={showNavigation}
                            onToggleTreeNav={this.handleToggleNavigation}
                            onRuleIdChange={this.handleRuleIdChange}
                            askForDiscardData={this.state.askForDiscard}
                            onAskDiscardChanges={this.toggleAskForDiscard}
                            onClickedRemove={this.handleClickRemove}
                        />
                    }
                </div>
            </section>
        );
    }
}

export const updateMappingEditorUrl = (currentUrl, newRule) => {
    const segments = currentUrl.segment();
    const transformIdx = segments.findIndex((segment) => segment === "transform");
    const editorIdx = transformIdx + 3;
    console.assert(segments[editorIdx] === "editor", "Wrong URL structure, 'editor not at correct position!'");
    for(let i = editorIdx + 1; i < segments.length; i++) {
        // Remove everything after "editor"
        currentUrl.segment(editorIdx + 1, "");
    }
    // add new rule suffix
    currentUrl.segment("rule");
    currentUrl.segment(newRule);
    return currentUrl.toString();
};

export default withHistoryHOC(HierarchicalMapping);
