
import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import hierarchicalMappingChannel from './store';
import TreeView from './Components/TreeView';
import {Button} from 'ecc-gui-elements';
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
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.onRuleNavigation);

        return {
            // currently selected rule id
            currentRuleId: undefined,
            // show / hide navigation
            showNavigation: true,
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
        return (
            <div
                className="ecc-component-hierarchicalMapping"
            >
                <div className="ecc-component-hierarchicalMapping__content">
                    <Button
                        className="ecc-component-hierarchicalMapping__content-toggle-navigation"
                        iconName={this.state.showNavigation ? 'arrow_prevpage' : 'more_vert'}
                        onClick={this.handleToggleNavigation}
                    />
                    {treeView}
                    <MappingRuleOverview
                        apiBase={this.props.apiBase}
                        project={this.props.project}
                        transformationTask={this.props.transformationTask}
                        currentRuleId={this.state.currentRuleId}
                    />
                </div>
            </div>
        );
    },
});

export default HierarchicalMapping;