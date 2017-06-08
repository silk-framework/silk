import React from 'react';
import UseMessageBus from './UseMessageBusMixin';
import hierarchicalMappingChannel from './store';
import _ from 'lodash';
import TreeView from './Components/TreeView';
import {ConfirmationDialog, AffirmativeButton, DismissiveButton, DisruptiveButton,Button, ContextMenu, MenuItem} from 'ecc-gui-elements';
import MappingRuleOverview from './Components/MappingRuleOverview'

// Do not care about it yet
/*const props = {
 apiBase: 'http://foo/bar',
 project: 'example',
 transformationTask: '',
 };*/

const HierarchicalMapping = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    /*propTypes: {
     apiBase: React.PropTypes.string.isRequired, // used restApi url
     project: React.PropTypes.string.isRequired, // used project name
     transformationTask: React.PropTypes.string, // used transformation
     },*/

    // initilize state
    getInitialState() {
        // listen to rule id changes
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.reload);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.onRuleNavigation);
        this.subscribe(hierarchicalMappingChannel.subject('removeClick'), this.handleClickRemove);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.edit'), this.onOpenEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.closed'), this.onCloseEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.create'), this.onOpenEdit);

        // listen to rule create event

        return {
            // currently selected rule id
            currentRuleId: 'root',
            // show / hide navigation
            showNavigation: true,
            // which edit view are we viewing
            ruleEditView: false,
            elementToDelete: false,
            editingElements: [],
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
    handleClickRemove({id, type}) {
        this.setState({
            elementToDelete: {id, type},
        });
    },
    handleConfirmRemove(event) {
        event.stopPropagation();
        hierarchicalMappingChannel.request({topic: 'rule.removeRule', data: {id: this.state.elementToDelete.id}})
            .subscribe(
                () => {
                    // FIXME: let know the user which element is gone!
                    this.setState({
                        elementToDelete: false,
                    });
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
        if (this.state.editingElements.length === 0) {
            this.setState({
                currentRuleId: newRuleId,
            });
        }
        else if (this.state.editingElements.length > 0 && confirm('Pressing ok will destroy all unsaved changes. \nAre you sure you want to continue?')){
            if (_.includes(this.state.editingElements, 0)) {
                hierarchicalMappingChannel.subject('ruleView.closed').onNext({id: 0});
            }
            this.setState({
                editingElements: [],
                currentRuleId: newRuleId,
            });
        }


    },
    // show / hide navigation
    handleToggleNavigation() {
        this.setState({
            showNavigation: !this.state.showNavigation,
        });
    },

    // template rendering
    render () {
        const ruleEdit = this.state.ruleEditView ? this.state.ruleEditView : {};
        const treeView = (
            this.state.showNavigation ? (
                <TreeView
                    apiBase={this.props.apiBase}
                    project={this.props.project}
                    transformationTask={this.props.transformationTask}
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
                Are you sure you want to delete the rule with id '{this.state.elementToDelete.id}' and type '{this.state.elementToDelete.type}'?
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
                    onClick = {() => {
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
                                apiBase={this.props.apiBase}
                                project={this.props.project}
                                transformationTask={this.props.transformationTask}
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
