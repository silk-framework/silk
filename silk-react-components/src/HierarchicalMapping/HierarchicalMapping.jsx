import React from 'react';
import _ from 'lodash';
import {
    Button,
    DisruptiveButton,
    Spinner,
} from '@eccenca/gui-elements';
import { URI } from 'ecc-utils';

import UseMessageBus from './UseMessageBusMixin';
import { getHierarchyAsync, ruleRemoveAsync, setApiDetails } from './store';

import MappingsTree from './Components/MappingsTree';
import MappingsWorkview from './Components/MappingsWorkview';
import MessageHandler from './Components/MessageHandler';
import { MAPPING_RULE_TYPE_OBJECT } from './helpers';
import { MESSAGES } from './constants';
import RemoveMappingRuleDialog from './elements/RemoveMappingRuleDialog';
import DiscardChangesDialog from './elements/DiscardChangesDialog';
import EventEmitter from './utils/EventEmitter';

const HierarchicalMapping = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        baseUrl: React.PropTypes.string.isRequired, // DI API Base
        project: React.PropTypes.string.isRequired, // Current DI Project
        transformTask: React.PropTypes.string.isRequired, // Current Transformation
        initialRule: React.PropTypes.string,
    },
    componentDidMount() {
        EventEmitter.on(MESSAGES.BUTTON.REMOVE_CLICK, this.handleClickRemove);
        EventEmitter.on(MESSAGES.RULE_VIEW.CHANGE, this.onOpenEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.UNCHANGED, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.CLOSE, this.onCloseEdit);
        EventEmitter.on(MESSAGES.RULE_VIEW.DISCARD_ALL, this.discardAll);
        EventEmitter.on(MESSAGES.RELOAD, this.loadNavigationTree);
        
        this.loadNavigationTree();
    },
    // initilize state
    getInitialState() {
        const {baseUrl, project, transformTask, initialRule} = this.props;
        setApiDetails({
            baseUrl,
            project,
            transformTask,
        });

        // TODO: Use initialRule
        return {
            // currently selected rule id
            currentRuleId: _.isEmpty(initialRule) ? undefined : initialRule,
            // show / hide navigation
            showNavigation: true,
            // which edit view are we viewing
            elementToDelete: false,
            editingElements: [],
            askForDiscard: false,

            // navigationTree
            navigationLoading: true,
            navigationTree: undefined,
            navigationExpanded: {},
        };
    },
    loadNavigationTree() {
        const { baseUrl, project, transformTask } = this.props;
        const { navigationExpanded } = this.state;
        this.setState({ navigationLoading: true });

        getHierarchyAsync({
            baseUrl,
            project,
            transformTask,
        })
            .subscribe(
                ({ hierarchy }) => {
                    const topLevelId = hierarchy.id;
                    this.setState({
                        navigationLoading: false,
                        navigationTree: hierarchy,
                        navigationExpanded: (_.isEmpty(navigationExpanded) && topLevelId)
                            ? { [topLevelId]: true }
                            : navigationExpanded,
                    });
                },
                () => {
                    this.setState({ navigationLoading: false });
                }
            );
    },
    expandNavigationTreeElement({ newRuleId, parentId }) {
        const expanded = { ...this.state.navigationExpanded };
        expanded[newRuleId] = true;
        expanded[parentId] = true;
        this.setState({ navigationExpanded: expanded });
    },
    // collapse / expand navigation children
    handleToggleExpandNavigationTree(id) {
        const expanded = { ...this.state.navigationExpanded };
        expanded[id] = !expanded[id];
        this.setState({ navigationExpanded: expanded });
    },

    onOpenEdit(obj) {
        const id = _.get(obj, 'id', 0);
        if (!_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.concat(this.state.editingElements, [id]),
            });
        }
    },
    onCloseEdit(obj) {
        const id = _.get(obj, 'id', 0);
        if (_.includes(this.state.editingElements, id)) {
            this.setState({
                editingElements: _.filter(
                    this.state.editingElements,
                    e => e !== id
                ),
            });
        }
    },
    handleClickRemove({
        id, uri, type, parent,
    }) {
        this.setState({
            editingElements: [],
            elementToDelete: {
                id, uri, type, parent,
            },
        });
    },
    handleConfirmRemove(event) {
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
                            elementToDelete: false,
                            loading: false,
                        });
                    } else {
                        this.setState({
                            elementToDelete: false,
                            loading: false,
                        });
                    }
                },
                err => {
                    // FIXME: let know the user what have happened!
                    this.setState({
                        elementToDelete: false,
                        loading: false,
                    });
                }
            );
    },
    handleCancelRemove() {
        this.setState({
            elementToDelete: false,
        });
    },
    // react to rule id changes
    onRuleNavigation({ newRuleId }) {
        if (newRuleId === this.state.currentRuleId) {
            // Do nothing!
        } else if (this.state.editingElements.length === 0) {
            this.setState({
                currentRuleId: newRuleId,
            });
        } else {
            this.setState({
                askForDiscard: newRuleId,
            });
        }
    },
    componentDidUpdate(prevProps, prevState) {
        if (
            prevState.currentRuleId !== this.state.currentRuleId &&
            !_.isEmpty(this.state.currentRuleId)
        ) {
            const href = window.location.href;

            try {
                const uriTemplate = new URI(href);

                if (uriTemplate.segment(-2) !== 'rule') {
                    uriTemplate.segment('rule');
                    uriTemplate.segment('rule');
                }

                uriTemplate.segment(-1, this.state.currentRuleId);
                history.pushState(null, '', uriTemplate.toString());
            } catch (e) {
                console.debug(`HierarchicalMapping: ${href} is not an URI, cannot update the window state`);
            }
        }
        if (prevProps.task !== this.props.task) {
            this.loadNavigationTree();
        }
    },
    // show / hide navigation
    handleToggleNavigation(stateVisibility) {
        this.setState({
            showNavigation: stateVisibility,
        });
    },
    handleDiscardChanges() {
        if (_.includes(this.state.editingElements, 0)) {
            EventEmitter.emit(MESSAGES.RULE_VIEW.UNCHANGED, { id: 0 })
        }
        this.setState({
            editingElements: [],
            currentRuleId: this.state.askForDiscard,
            askForDiscard: false,
        });
        EventEmitter.emit(MESSAGES.RULE_VIEW.DISCARD_ALL);
    },
    discardAll() {
        this.setState({
            editingElements: [],
        });
    },
    handleCancelDiscard() {
        this.setState({ askForDiscard: false });
    },
    handleRuleIdChange(rule) {
        this.onRuleNavigation(rule);
        this.expandNavigationTreeElement(rule);
    },
    // template rendering
    render() {
        const {
            navigationLoading, navigationTree, navigationExpanded, currentRuleId, showNavigation,
            elementToDelete, askForDiscard, editingElements,
        } = this.state;
        const loading = this.state.loading ? <Spinner /> : false;

        // render mapping edit / create view of value and object
        const debugOptions = __DEBUG__ ? (
            <div>
                <DisruptiveButton
                    onClick={() => {
                        localStorage.setItem('mockStore', null);
                        location.reload();
                    }}
                >
                    RESET
                </DisruptiveButton>
                <Button
                    onClick={() => {
                        EventEmitter.emit(MESSAGES.RELOAD, true);
                    }}
                >
                    RELOAD
                </Button>
                <hr />
            </div>
        ) : (
            false
        );
        const messagequeue = <MessageHandler />;
        const pseudotoasts = messagequeue ? (
            <div className="ecc-temp__appmessages">{messagequeue}</div>
        ) : (
            false
        );
        return (
            <section className="ecc-silk-mapping">
                {debugOptions}
                {
                    elementToDelete && (
                        <RemoveMappingRuleDialog
                            mappingType={elementToDelete.type}
                            handleConfirmRemove={this.handleConfirmRemove}
                            handleCancelRemove={this.handleCancelRemove}
                        />
                    )
                }
                {
                    askForDiscard && (
                        <DiscardChangesDialog
                            handleDiscardConfirm={this.handleDiscardChanges}
                            handleDiscardCancel={this.handleCancelDiscard}
                            numberEditingElements={editingElements.length}
                        />
                    )
                }
                {loading}
                {pseudotoasts}
                <div className="ecc-silk-mapping__content">
                    {
                        showNavigation && (
                            <MappingsTree
                                currentRuleId={currentRuleId}
                                navigationLoading={navigationLoading}
                                navigationTree={navigationTree}
                                navigationExpanded={navigationExpanded}
                                handleRuleNavigation={this.onRuleNavigation}
                                handleToggleExpanded={this.handleToggleExpandNavigationTree}
                            />
                        )
                    }
                    {
                        <MappingsWorkview
                            currentRuleId={this.state.currentRuleId}
                            onToggleTreeNav={this.handleToggleNavigation}
                            onRuleIdChange={this.handleRuleIdChange}
                        />
                    }
                </div>
            </section>
        );
    },
});

export default HierarchicalMapping;
