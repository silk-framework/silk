import React from 'react';
import _ from 'lodash';
import {
    Button,
    DismissiveButton,
    DisruptiveButton,
    Card,
    CardTitle,
    ContextMenu,
    MenuItem,
    ConfirmationDialog,
    Spinner,
} from '@eccenca/gui-elements';
import {URI} from 'ecc-utils';

import UseMessageBus from './UseMessageBusMixin';
import hierarchicalMappingChannel from './store';

import MappingsTree from './Components/MappingsTree';
import MappingsWorkview from './Components/MappingsWorkview';
import MessageHandler from './Components/MessageHandler';
import {MAPPING_RULE_TYPE_OBJECT} from './helpers';
import { MESSAGES } from './constants';

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
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_ID.CHANGE),
            this.onRuleNavigation
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.BUTTON.REMOVE_CLICK),
            this.handleClickRemove
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CHANGE),
            this.onOpenEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.UNCHANGED),
            this.onCloseEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.CLOSE),
            this.onCloseEdit
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.DISCARD_ALL),
            this.discardAll
        );
        this.subscribe(
            hierarchicalMappingChannel.subject(MESSAGES.TREE_NAV.TOGGLE_VISIBILITY),
            this.handleToggleNavigation
        );
    },
    // initilize state
    getInitialState() {
        const {baseUrl, project, transformTask, initialRule} = this.props;

        hierarchicalMappingChannel.subject(MESSAGES.SILK.SET_DETAILS).onNext({
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
        };
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
    handleClickRemove({id, uri, type, parent}) {
        this.setState({
            editingElements: [],
            elementToDelete: {id, uri, type, parent},
        });
    },
    handleConfirmRemove(event) {
        event.stopPropagation();
        const {parent, type} = this.state.elementToDelete;
        this.setState({
            loading: true,
        });
        hierarchicalMappingChannel
            .request({
                topic: MESSAGES.RULE.REMOVE,
                data: {...this.state.elementToDelete},
            })
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
    onRuleNavigation({newRuleId}) {
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
                console.debug(
                    `HierarchicalMapping: ${href} is not an URI, cannot update the window state`
                );
            }
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
            hierarchicalMappingChannel
                .subject(MESSAGES.RULE_VIEW.UNCHANGED)
                .onNext({id: 0});
        }
        this.setState({
            editingElements: [],
            currentRuleId: this.state.askForDiscard,
            askForDiscard: false,
        });
        hierarchicalMappingChannel.subject(MESSAGES.RULE_VIEW.DISCARD_ALL).onNext();
    },
    discardAll() {
        this.setState({
            editingElements: [],
        });
    },
    handleCancelDiscard() {
        this.setState({askForDiscard: false});
    },
    // template rendering
    render() {
        const navigationTree = this.state.showNavigation ? (
            <MappingsTree
                baseUrl={this.props.baseUrl}
                project={this.props.project}
                task={this.props.transformTask}
                currentRuleId={this.state.currentRuleId} />
        ) : (
            false
        );
        const loading = this.state.loading ? <Spinner /> : false;
        const deleteView = this.state.elementToDelete ? (
            <ConfirmationDialog
                className="ecc-hm-delete-dialog"
                active
                modal
                title="Remove mapping rule?"
                confirmButton={
                    <DisruptiveButton
                        className="ecc-hm-delete-accept"
                        disabled={false}
                        onClick={this.handleConfirmRemove}>
                        Remove
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton
                        className="ecc-hm-delete-cancel"
                        onClick={this.handleCancelRemove}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>
                    When you click REMOVE the mapping rule
                    {this.state.elementToDelete.type ===
                    MAPPING_RULE_TYPE_OBJECT
                        ? ' including all child rules '
                        : ' '}
                    will be deleted permanently.
                </p>
            </ConfirmationDialog>
        ) : (
            false
        );

        const discardView = this.state.askForDiscard ? (
            <ConfirmationDialog
                active
                modal
                className="ecc-hm-discard-dialog"
                title="Discard changes?"
                confirmButton={
                    <DisruptiveButton
                        disabled={false}
                        className="ecc-hm-accept-discard"
                        onClick={this.handleDiscardChanges}>
                        Discard
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton
                        className="ecc-hm-cancel-discard"
                        onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>
                    You currently have unsaved changes{this.state
                        .editingElements.length === 1
                        ? ''
                        : ` in ${
                              this.state.editingElements.length
                          } mapping rules`}.
                </p>
            </ConfirmationDialog>
        ) : (
            false
        );

        // render mapping edit / create view of value and object
        const debugOptions = __DEBUG__ ? (
            <div>
                <DisruptiveButton
                    onClick={() => {
                        localStorage.setItem('mockStore', null);
                        location.reload();
                    }}>
                    RESET
                </DisruptiveButton>
                <Button
                    onClick={() => {
                        hierarchicalMappingChannel
                            .subject(MESSAGES.RELOAD)
                            .onNext(true);
                    }}>
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
                {deleteView}
                {discardView}
                {loading}
                {pseudotoasts}
                <div className="ecc-silk-mapping__content">
                    {navigationTree}
                    {
                        <MappingsWorkview
                            currentRuleId={this.state.currentRuleId}
                        />
                    }
                </div>
            </section>
        );
    },
});

export default HierarchicalMapping;
