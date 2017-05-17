/*
* Tree View On The Left
*/

import React from 'react';
import {UseMessageBus} from 'ecc-mixins';
import {Spinner, Button} from 'ecc-gui-elements';
import _ from 'lodash';
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
            // list of expanded navigation parents
            expanded: {},
        };
    },

    componentDidMount() {
        // get navigation tree data
        hierarchicalMappingChannel.request({topic: 'hierarchy.get'})
        .subscribe(
            ({hierarchy}) => {
                // expand root level
                let topLevelId = _.get(hierarchy, 'id');
                this.setState({
                    loading: false,
                    tree: hierarchy,
                    expanded: topLevelId ? {[topLevelId]: true} : {},
                });
            },
            (err) => {
                console.warn('err TreeView: hierarchy.get');
                this.setState({loading: false});
            }
        );
    },
    // select clicked id
    handleNavigate(ruleId) {
        hierarchicalMappingChannel.subject('ruleId.change').onNext({newRuleId: ruleId});
    },

    // collapse / expand navigation childs
    handleToggleExpanded(id) {
        console.warn('debug id', id);
        // copy
        const expanded = _.cloneDeep(this.state.expanded);
        // get id state
        const currentlyExpanded = _.get(expanded, [id], false);
        // negate state
        expanded[id] = !currentlyExpanded;
        this.setState({expanded});
    },

    // template rendering
    render () {
        // construct parent-child tree
        const navigationList = ({parent, root}) => {
            const {id, name, rules, type} = parent;
            const childs = (
                _.chain(rules)
                .cloneDeep()
                .filter({type: 'hierarchical'})
                .value()
            );
            // get expanded state
            const expanded = _.get(this.state, ['expanded', id], false);
            // check if this element is selected (select root if no selected exist)
            const isHighlighted = id === this.props.currentRuleId || (root && _.isUndefined(this.props.currentRuleId));
            const element = (isHighlighted) => (
                <Button
                    onClick={() => {this.handleNavigate(id)}}
                >
                    {isHighlighted ? (<b>{name}</b>) : name}
                </Button>
            );
            return (
                <ul>
                    {
                        !_.isEmpty(childs) ? (
                            <Button
                                iconName={expanded ? 'expand_less' : 'expand_more'}
                                onClick={() => {this.handleToggleExpanded(id)}}
                            />
                        ) : false
                    }
                    {element(isHighlighted)}
                    {
                        expanded ? (
                            _.map(childs, (child, idx) => (
                                <li key={name + '.' + idx}>
                                    {navigationList({parent: child})}
                                </li>
                            ))
                        ) : false
                    }
                </ul>
            )

        };

        const content = (
            !_.isEmpty(this.state.tree) ? (
                    navigationList({parent: this.state.tree, root: true})
            ) : false
        );

        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div
                className="ecc-component-hierarchicalMapping__content-treeView"
            >
                {loading}
                {content}
            </div>
        );
    },
});

export default TreeView;