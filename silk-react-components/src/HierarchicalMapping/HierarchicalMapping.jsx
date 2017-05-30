import React from 'react';
import UseMessageBus from './UseMessageBusMixin';
import hierarchicalMappingChannel from './store';
import TreeView from './Components/TreeView';
import {Button, ContextMenu, MenuItem} from 'ecc-gui-elements';
import MappingRuleOverview from './Components/MappingRuleOverview'
import RuleValueEdit from './Components/RuleValueEditView';
import RuleObjectEdit from './Components/RuleObjectEditView';

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
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.onRuleNavigation);
        // listen to rule create event
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.create'), this.onRuleCreate);

        return {
            // currently selected rule id
            currentRuleId: undefined,
            // show / hide navigation
            showNavigation: true,
            // show / hide edit view of rule
            // TODO: set to false as default after developing
            ruleEditView: {
                type: false, // or type: 'direct' | 'object'
            },
        };
    },
    // react to rule id changes
    onRuleNavigation({newRuleId}) {
        this.setState({
            currentRuleId: newRuleId,
        })
    },
    // show / hide navigation
    handleToggleNavigation() {
        this.setState({
            showNavigation: !this.state.showNavigation,
        });
    },
    onRuleCreate({type}) {
        this.setState({
            ruleEditView: {
                type,
            },
        });
    },
    handleRuleEditClose() {
        this.setState({
            ruleEditView: false,
        });
    },
    // template rendering
    render () {

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
        // render mapping edit / create view of value and object
        const editView = () => {
            if (this.state.ruleEditView) {
                return (
                    this.state.ruleEditView.type === 'object' ? (
                        <RuleObjectEdit
                            {...this.state.ruleEditView}
                            onClose={this.handleRuleEditClose}
                            parentId={this.state.currentRuleId}
                            edit={true}
                        />
                    ) : (
                        <RuleValueEdit
                            {...this.state.ruleEditView}
                            onClose={this.handleRuleEditClose}
                            parentId={this.state.currentRuleId}
                            edit={true}
                        />
                    )
                )
            }
            return false;
        };

        return (
            <div
                className="ecc-silk-mapping"
            >
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch">
                    <div className="ecc-silk-mapping__header mdl-card__title">
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
                        <MappingRuleOverview
                            apiBase={this.props.apiBase}
                            project={this.props.project}
                            transformationTask={this.props.transformationTask}
                            currentRuleId={this.state.currentRuleId}
                        />
                        {/*TODO: CreateView should be placed here but editView need to be part of mapping rule list*/}
                        {editView()}
                    </div>
                </div>
            </div>
        );
    },
});

export default HierarchicalMapping;
