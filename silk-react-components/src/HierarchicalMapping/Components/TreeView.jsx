/*
 * Tree View On The Left
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
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
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.loadData);
        return {
            loading: true,
            tree: undefined,
            // list of expanded navigation parents
            expanded: {},
        };
    },
    componentDidMount() {
        this.loadData();
    },
    loadData(){
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
            const {id, rules = {}, type} = parent;
            const childs = (
                _.chain(rules.propertyRules)
                    .cloneDeep()
                    .filter(({type}) => type === 'object')
                    .value()
            );
            // get expanded state
            const expanded = _.get(this.state, ['expanded', id], false);
            // check if this element is selected (select root if no selected exist)
            const isHighlighted = id === this.props.currentRuleId || (type === 'root' && _.isUndefined(this.props.currentRuleId));
            const element = () => {

                const types = _.get(parent, 'rules.typeRules', []).map(({typeUri}) => typeUri);


                return (
                    <span
                        onClick={() => {
                            this.handleNavigate(id)
                        }}
                    >
                        {type === 'object' ? _.get(parent, 'mappingTarget.uri', '(no target property)') : 'Root'}<br/>
                        <small>{_.isEmpty(types) ? '(no target types)' : types.join(', ')}</small>
                    </span>
                );
            }
            return (
                <ul className={`ecc-component-hierarchicalMapping__content-treeView-${isHighlighted ? 'highlight' : ''}` }>
                    {
                        !_.isEmpty(childs) ? (
                            <Button
                                iconName={expanded ? 'expand_more' : 'arrow_nextpage'}
                                tooltip={expanded ? 'Close tree' : 'Open tree'}
                                onClick={() => {
                                    this.handleToggleExpanded(id)
                                }}
                            />
                        ) : false
                    }
                    {element()}
                    {
                        expanded ? (
                            _.map(childs, (child, idx) => (
                                <li key={id + '.' + idx}>
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
                <div className="mdl-card mdl-shadow--2dp mdl-card--stretch stretch-vertical">
                    {loading}
                    {content}
                </div>
            </div>
        );
    },
});

export default TreeView;
