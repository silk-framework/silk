import React from 'react';
import UseMessageBus from './UseMessageBusMixin';
import hierarchicalMappingChannel from './store';
import _ from 'lodash';
import TreeView from './Components/TreeView';
import {ConfirmationDialog, AffirmativeButton, DismissiveButton, DisruptiveButton,Button, ContextMenu, MenuItem} from 'ecc-gui-elements';
import MappingRuleOverview from './Components/MappingRuleOverview'

const HierarchicalMapping = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        baseUrl: React.PropTypes.string.isRequired, // DI API Base
        project: React.PropTypes.string.isRequired, // Current DI Project
        transformTask: React.PropTypes.string.isRequired, //Current Transformation
        initialRule: React.PropTypes.string,
     },

    // initilize state
    getInitialState() {

        const {
            baseUrl,
            project,
            transformTask,
            initialRule,
        } = this.props;

        hierarchicalMappingChannel.subject('setSilkDetails').onNext({
            baseUrl,
            project,
            transformTask,
        });

        // listen to rule id changes
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.reload);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.onRuleNavigation);
        this.subscribe(hierarchicalMappingChannel.subject('removeClick'), this.handleClickRemove);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.edit'), this.onOpenEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.closed'), this.onCloseEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.create'), this.onOpenEdit);

        // listen to rule create event

        //TODO: Use initialRule
        return {
            // currently selected rule id
            currentRuleId: 'root',
            // show / hide navigation
            showNavigation: true,
            // which edit view are we viewing
            ruleEditView: false,
            elementToDelete: false,
            editingElements: [],
            askForDiscard: false,
        };
    },
    onOpenEdit(obj) {
        const id = _.get(obj, 'id', 0);
        this.setState({
            editingElements: _.merge(this.state.editingElements, [id]),
        });
    },
    onCloseEdit(obj) {
        const id = _.get(obj, 'id', 0);
        if (!_.includes( ))
        this.setState({
            editingElements: _.filter(this.state.editingElements, (e) => e !== id),
        })
    },
    handleClickRemove({id, type, parent}) {
        this.setState({
                editingElements: [],
                elementToDelete: {id, type, parent},
        });

    },
    handleConfirmRemove(event) {
        event.stopPropagation();
        const parent = this.state.elementToDelete.parent;
        const type = this.state.elementToDelete.type;
        hierarchicalMappingChannel.request({topic: 'rule.removeRule', data: {...this.state.elementToDelete}})
            .subscribe(
                () => {
                    // FIXME: let know the user which element is gone!
                    if (type === 'object') {
                        this.setState({
                            currentRuleId: parent,
                            elementToDelete: false,
                        });
                    }
                    else{
                        this.setState({
                            elementToDelete: false,
                        });
                    }
                },
                (err) => {
                    // FIXME: let know the user what have happened!
                    this.setState({
                        elementToDelete: false,
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
        }
        else if (this.state.editingElements.length === 0) {
            this.setState({
                currentRuleId: newRuleId,
            });
        }
        else {
            this.setState({
                askForDiscard: newRuleId
            });
       }
    },
    // show / hide navigation
    handleToggleNavigation() {
        this.setState({
            showNavigation: !this.state.showNavigation,
        });
    },
    handleDiscardChanges() {
        if (_.includes(this.state.editingElements, 0)) {
            hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: 0});
        }
        this.setState({
            editingElements: [],
            currentRuleId: this.state.askForDiscard,
            askForDiscard: false,
        });

    },
    handleCancelDiscard() {
        this.setState({askForDiscard: false});
    },
    // template rendering
    render () {
        const ruleEdit = this.state.ruleEditView ? this.state.ruleEditView : {};
        const treeView = (
            this.state.showNavigation ? (
                <TreeView
                    currentRuleId={this.state.currentRuleId}
                />
            ) : false
        );

        const deleteView = this.state.elementToDelete
            ? <ConfirmationDialog
                active={true}
                title="Delete Rule"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleConfirmRemove}>
                        Delete
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelRemove}>
                        Cancel
                    </DismissiveButton>
                }>
                Clicking on Delete will delete the current mapping rule
                {this.state.elementToDelete.type === 'object'
                    ? " as well as all existing children rules. "
                    :'. '
                }
                Are you sure you want to delete the rule with id '{this.state.elementToDelete.id}' and
                type '{this.state.elementToDelete.type}'?

            </ConfirmationDialog>
            : false;

        const discardView = this.state.askForDiscard
            //confirm('')
            ? <ConfirmationDialog
                active={true}
                title="Discard changes"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleDiscardChanges}>
                        Continue
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelDiscard}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>By clicking on CONTINUE, all unsaved changes will be destroy.</p><p>Are you sure you want to continue?</p>
            </ConfirmationDialog>
            : false;


        // render mapping edit / create view of value and object
        const debugOptions = __DEBUG__
            ? (<div>
                <DisruptiveButton
                    onClick={() => {
                        localStorage.setItem('mockStore', null);
                        location.reload();
                    }}
                >RESET</DisruptiveButton>
                <Button
                    onClick={() => {
                        hierarchicalMappingChannel.subject('reload').onNext(true);
                    }}
                >RELOAD</Button></div>) : false;

        return (
            <div
                className="ecc-silk-mapping"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch">
                    <div className="ecc-silk-mapping__header mdl-card__title">
                        {debugOptions}
                        {deleteView}
                        {discardView}
                        <ContextMenu
                            iconName="tune"
                        >
                            <MenuItem
                                onClick={this.handleToggleNavigation}
                            >
                                {this.state.showNavigation ? 'Hide tree navigation' : 'Show tree navigation'}
                            </MenuItem>
                        </ContextMenu>
                    </div>
                    <div className="ecc-silk-mapping__content">
                        {treeView}
                        {
                            <MappingRuleOverview
                                currentRuleId={this.state.currentRuleId}
                                ruleEditView={{...ruleEdit}}
                            />
                        }
                    </div>
                </div>
            </div>
        );
    },
});

export default HierarchicalMapping;
