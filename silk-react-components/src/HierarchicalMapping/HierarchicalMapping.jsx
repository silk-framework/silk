import React from 'react';
import UseMessageBus from './UseMessageBusMixin';
import hierarchicalMappingChannel from './store';
import _ from 'lodash';
import TreeView from './Components/TreeView';
import {
    Spinner,
    ConfirmationDialog,
    DismissiveButton,
    DisruptiveButton,
    Button,
    ContextMenu,
    MenuItem,
} from 'ecc-gui-elements';

import {
    ThingName
} from './Components/MappingRule/SharedComponents';

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
    componentDidMount(){
        // listen to rule id changes
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.onRuleNavigation);
        this.subscribe(hierarchicalMappingChannel.subject('removeClick'), this.handleClickRemove);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.change'), this.onOpenEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.unchanged'), this.onCloseEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.close'), this.onCloseEdit);
        this.subscribe(hierarchicalMappingChannel.subject('ruleView.discardAll'), this.discardAll);
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


        //TODO: Use initialRule
        return {
            // currently selected rule id
            currentRuleId: 'root',
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
                editingElements: _.filter(this.state.editingElements, (e) => e !== id),
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
        const {parent, type}  = this.state.elementToDelete;
        this.setState({
            loading: true,
        });
        hierarchicalMappingChannel.request({topic: 'rule.removeRule', data: {...this.state.elementToDelete}})
            .subscribe(
                () => {
                    // FIXME: let know the user which element is gone!
                    if (type === 'object') {
                        this.setState({
                            currentRuleId: parent,
                            elementToDelete: false,
                            loading: false,
                        });
                    }
                    else{
                        this.setState({
                            elementToDelete: false,
                            loading: false,
                        });
                    }
                },
                (err) => {
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
            hierarchicalMappingChannel.subject('ruleView.unchanged').onNext({id: 0});
        }
        this.setState({
            editingElements: [],
            currentRuleId: this.state.askForDiscard,
            askForDiscard: false,
        });
        hierarchicalMappingChannel.subject('ruleView.discardAll').onNext();
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
    render () {
        const treeView = (
            this.state.showNavigation ? (
                <TreeView
                    currentRuleId={this.state.currentRuleId}
                />
            ) : false
        );
        const loading = this.state.loading ? <Spinner/> : false;
        const deleteView = this.state.elementToDelete
            ? <ConfirmationDialog
                active={true}
                title="Remove mapping rule?"
                confirmButton={
                    <DisruptiveButton disabled={false} onClick={this.handleConfirmRemove}>
                        Remove
                    </DisruptiveButton>
                }
                cancelButton={
                    <DismissiveButton onClick={this.handleCancelRemove}>
                        Cancel
                    </DismissiveButton>
                }>
                <p>
                    The {this.state.elementToDelete.type} mapping rule for <ThingName id={this.state.elementToDelete.uri} />
                    {this.state.elementToDelete.type === 'object'
                        ? " and all its children rules are "
                        :' is '
                    }
                    going to be removed permanently.
                </p>
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
                <p>{this.state.editingElements.length} edit form{this.state.editingElements.length>1?'s':''} containing changes {this.state.editingElements.length>1?'are':'is'} still open. Navigation will discard these changes.</p>
                <p>Are you sure you want to continue?</p>
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
                        {loading}
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
                            />
                        }
                    </div>
                </div>
            </div>
        );
    },
});

export default HierarchicalMapping;
