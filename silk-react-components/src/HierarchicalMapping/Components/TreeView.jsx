/*
 * Tree View On The Left
 */

import React from 'react';
import UseMessageBus from '../UseMessageBusMixin';
import {Spinner, Button, Icon} from 'ecc-gui-elements';
import _ from 'lodash';
import hierarchicalMappingChannel from '../store';
import {RuleTreeTitle, RuleTreeTypes} from './MappingRule/SharedComponents';

const TreeView = React.createClass({

    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        currentRuleId: React.PropTypes.string, // currently selected rule id (tree highlighting)
    },

    // initilize state
    getInitialState() {
        this.subscribe(hierarchicalMappingChannel.subject('reload'), this.loadData);
        this.subscribe(hierarchicalMappingChannel.subject('ruleId.change'), this.expandElement);
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
    expandElement({parent}){
        if (!_.isUndefined(parent) && !_.includes(this.state.expanded, {[parent]:true})) {
            this.setState({
                expanded: _.merge(this.state.expanded, {[parent]: true}),
            })
        }
    },
    loadData(){

        console.warn('TREE RELOAD')

        // get navigation tree data
        hierarchicalMappingChannel.request({topic: 'hierarchy.get'})
            .subscribe(
                ({hierarchy}) => {
                    // expand root level
                    let topLevelId = _.get(hierarchy, 'id');
                    this.setState({
                        loading: false,
                        tree: hierarchy,
                        expanded: _.isEmpty(this.state.expanded) && topLevelId
                            ? {[topLevelId]: true}
                            : this.state.expanded,
                    });
                },
                (err) => {
                    console.warn('err TreeView: hierarchy.get', err);
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

                return (
                    <button
                        className="ecc-silk-mapping__treenav--item-handler"
                        onClick={() => {
                            this.handleNavigate(id)
                        }}
                    >
                        <span className="ecc-silk-mapping__treenav--item-maintitle">
                            <RuleTreeTitle rule={parent}/>
                        </span>
                        {(
                            type === 'object' ? (
                                <small className="ecc-silk-mapping__treenav--item-subtitle">
                                    {<RuleTreeTypes rule={parent}/>}
                                </small>
                            ) : false
                        )}
                    </button>
                );
            }
            return (
                <div>
                    <div className={
                            'ecc-silk-mapping__treenav--item' +
                            (isHighlighted ? ' ecc-silk-mapping__treenav--item-active' : '')
                    }
                    >
                        {
                            !_.isEmpty(childs) ? (
                                <Button
                                    className="ecc-silk-mapping__treenav--item-toggler"
                                    iconName={expanded ? 'expand_more' : 'arrow_nextpage'}
                                    tooltip={expanded ? 'Hide sub tree' : 'Open sub tree'}
                                    onClick={() => {
                                        this.handleToggleExpanded(id)
                                    }}
                                />
                            ) : (
                                <Icon
                                    className="ecc-silk-mapping__treenav--item-toggler"
                                    name="radio_button_unchecked"
                                    tooltip=""
                                />
                            )
                        }
                        {element()}
                    </div>
                    {
                        expanded ? (
                            <ul
                                className="ecc-silk-mapping__treenav--subtree"
                            >
                                {
                                    _.map(childs, (child, idx) => (
                                        <li
                                            key={id + '.' + idx}
                                        >
                                            {navigationList({parent: child})}
                                        </li>
                                    ))
                                }
                            </ul>
                        ) : false
                    }
                </div>
            );

        };

        const content = (
            !_.isEmpty(this.state.tree) ? (
                <ul className="ecc-silk-mapping__treenav--maintree">
                    <li>
                        {navigationList({parent: this.state.tree, root: true})}
                    </li>
                </ul>
            ) : false
        );

        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div
                className="ecc-silk-mapping__treenav"
            >
                {loading}
                {content}
            </div>
        );
    },
});

export default TreeView;
