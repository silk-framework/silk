/*
* Tree View On The Left
*/

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Spinner} from 'ecc-gui-elements';
import hierarchicalMappingChannel from '../store';

const TreeView = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
     //apiBase: React.PropTypes.string.isRequired, // used restApi url
     //project: React.PropTypes.string.isRequired, // used project name
     //transformationTask: React.PropTypes.string, // used transformation
     currentRuleId: React.PropTypes.string, // currently selected rule id (tree highlighting)
     },

    // initilize state
    getInitialState() {
        return {
            loading: true,
            tree: undefined,
        };
    },

    componentDidMount() {
        hierarchicalMappingChannel.request({topic: 'hierarchy.get'})
        .subscribe(
            ({hierarchy}) => {
                this.setState({
                    loading: false,
                    tree: hierarchy,
                });
            },
            (err) => {
                console.warn('err TreeView: hierarchy.get');
                this.setState({loading: false});
            }
        );
    },

    // template rendering
    render () {
        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div
                className="ecc-component-hierarchicalMapping__treeView"
            >
                {loading}
                Hello DI. I am TreeView.
                <br/>
                Selected Rule: {this.props.currentRuleId}
                <br/>
                Tree: {JSON.stringify(this.state.tree, null, 2)}
            </div>
        );
    },
});

export default TreeView;