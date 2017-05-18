
import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import hierarchicalMappingChannel from './store';
import TreeView from './Components/TreeView';
import {Button} from 'ecc-gui-elements';
import MappingRuleOverview from './Components/MappingRuleOverview'
import RuleEdit from './Components/RuleEditView';

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
        // listen to rule edit event
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.edit'), this.onRuleEdit);

        return {
            // currently selected rule id
            currentRuleId: undefined,
            // show / hide navigation
            showNavigation: true,
            // show / hide edit view of rule
            // TODO: set to false as default after developing
            ruleEditView: {
                id: 'XYZ_3',
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
    // show edit view of specific rule id
    onRuleEdit({rule}) {
        this.setState({
            ruleEditView: rule,
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
            this.state.showNavigation ?  (
                <TreeView
                    apiBase={this.props.apiBase}
                    project={this.props.project}
                    transformationTask={this.props.transformationTask}
                    currentRuleId={this.state.currentRuleId}
                />
            ) : false
        );

        const editView = (
            this.state.ruleEditView ? (
                <RuleEdit
                    {...this.state.ruleEditView}
                    onClose={this.handleRuleEditClose}
                />
            ) : false
        );

        return (
            <div
                className="ecc-component-hierarchicalMapping"
            >
                <div className="ecc-component-hierarchicalMapping__content">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-toggle-navigation"
                        iconName={this.state.showNavigation ? 'arrow_prevpage' : 'more_vert'}
                        tooltip={this.state.showNavigation ? 'Close tree' : 'Open tree'}
                        onClick={this.handleToggleNavigation}
                    />
                    {treeView}
                    <MappingRuleOverview
                        apiBase={this.props.apiBase}
                        project={this.props.project}
                        transformationTask={this.props.transformationTask}
                        currentRuleId={this.state.currentRuleId}
                    />
                    {/*TODO: move editView to correct position*/}
                    {editView}
                </div>
            </div>
        );
    },
});

export default HierarchicalMapping;