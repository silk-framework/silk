/*
 * Navigation tree over full hierarchical depth of mappings
 */

import React from 'react';
import _ from 'lodash';
import {
    Spinner,
    Button,
    Icon,
    Card,
    CardTitle,
    CardContent,
} from '@eccenca/gui-elements';

import Navigation from '../Mixins/Navigation';
import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel from '../store';
import {RuleTreeTitle, RuleTreeTypes} from './MappingRule/SharedComponents';
import {MAPPING_RULE_TYPE_OBJECT, MAPPING_RULE_TYPE_ROOT} from '../helpers';

const MappingsTree = React.createClass({
    mixins: [UseMessageBus, Navigation],

    // define property types
    propTypes: {
        // currently selected rule id (tree highlighting)
        currentRuleId: React.PropTypes.string,
        // Show value mappings in the tree
        showValueMappings: React.PropTypes.bool,
        // For each rule id, contains one of the following: "ok", "warning"
        ruleValidation: React.PropTypes.objectOf(React.PropTypes.oneOf(['ok', 'warning'])),
    },

    defaultProps: {
        showValueMappings: false,
        ruleValidation: {}
    },

    // initilize state
    getInitialState() {
        this.subscribe(
            hierarchicalMappingChannel.subject('reload'),
            this.loadData
        );
        this.subscribe(
            hierarchicalMappingChannel.subject('ruleId.change'),
            this.expandElement
        );
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
    expandElement({newRuleId, parentId}) {
        const expanded = this.state.expanded;
        expanded[newRuleId] = true;
        expanded[parentId] = true;
        this.setState({expanded});
    },
    loadData() {
        if (__DEBUG__) {
            console.warn('TREE RELOAD');
        }

        // get navigation tree data
        hierarchicalMappingChannel.request({topic: 'hierarchy.get'}).subscribe(
            ({hierarchy}) => {
                // expand root level
                const topLevelId = _.get(hierarchy, 'id');
                this.setState({
                    loading: false,
                    tree: hierarchy,
                    expanded:
                        _.isEmpty(this.state.expanded) && topLevelId
                            ? this.initialExpandedRules(hierarchy)
                            : this.state.expanded,
                });
            },
            err => {
                this.setState({loading: false});
            }
        );
    },

    initialExpandedRules(tree) {
        let expanded = {};
        if(this.props.hasOwnProperty('ruleValidation')) {
            expanded = this.computeExpandedRules(tree)
        }
        expanded[_.get(tree, 'id')] = true;
        return expanded;
    },

    /**
     * Returns an object which contains a key for each rule that should be expanded because it contains a child with a warning.
     * @param tree The rule tree
     */
    computeExpandedRules(tree) {
        let expanded = {};

        if (_.has(tree, 'rules.propertyRules')) {
            // Iterate all children
            _.forEach(tree.rules.propertyRules, rule => {
                // Collect all children
                expanded = {...expanded, ...this.computeExpandedRules(rule)};
                // Expand if a child contains a warning
                if (this.props.ruleValidation[rule.id] === "warning") {
                    expanded[tree.id] = true;
                }
            });
        }

        // Expand this node if at least one child is expanded
        if(!_.isEmpty(expanded)) {
            expanded[tree.id] = true;
        }

        return expanded;
    },

    // collapse / expand navigation children
    handleToggleExpanded(id) {
        const expanded = this.state.expanded;
        expanded[id] = !expanded[id];
        this.setState({expanded});
    },
    markTree(curr) {
        if (_.isEmpty(curr)) {
            return curr;
        }

        const tree = _.cloneDeep(curr);

        const {id, type} = tree;

        let expanded = _.get(this.state, ['expanded', id], false);
        let isHighlighted =
            id === this.props.currentRuleId ||
            (_.get(tree, 'rules.uriRule.id') === this.props.currentRuleId &&
                !_.isUndefined(this.props.currentRuleId)) ||
            (type === MAPPING_RULE_TYPE_ROOT &&
                _.isUndefined(this.props.currentRuleId));

        if (_.has(tree, 'rules.propertyRules')) {
            tree.rules.propertyRules = _.map(tree.rules.propertyRules, rule => {
                const subtree = this.markTree(rule);

                if (
                    subtree.type !== MAPPING_RULE_TYPE_OBJECT &&
                    subtree.id === this.props.currentRuleId
                ) {
                    isHighlighted = true;
                    expanded = true;
                }

                return subtree;
            });
        }

        tree.expanded = expanded;
        tree.isHighlighted = isHighlighted;

        return tree;
    },
    // template rendering
    render() {
        const tree = this.markTree(_.cloneDeep(this.state.tree));

        // construct parent-child tree
        const navigationList = ({parent}) => {
            const {
                id,
                type: parentType,
                rules = {},
                isHighlighted,
                expanded,
            } = parent;

            // get expanded state
            const childs = _.chain(rules.propertyRules)
                .filter(({type}) => this.props.showValueMappings || type === MAPPING_RULE_TYPE_OBJECT)
                .value();

            const element = () => (
                <button
                    className="ecc-silk-mapping__treenav--item-handler"
                    onClick={this.handleNavigate.bind(null, id, undefined)}>
                    <span className="ecc-silk-mapping__treenav--item-maintitle">
                        <RuleTreeTitle rule={parent} />
                        { this.renderRuleIcon(id) }
                    </span>
                    {parentType === MAPPING_RULE_TYPE_OBJECT ? (
                        <small className="ecc-silk-mapping__treenav--item-subtitle">
                            {<RuleTreeTypes rule={parent} />}
                        </small>
                    ) : (
                        false
                    )}
                </button>
            );

            return (
                <div>
                    <div
                        className={`ecc-silk-mapping__treenav--item${
                            isHighlighted
                                ? ' ecc-silk-mapping__treenav--item-active'
                                : ''
                        }`}>
                        {!_.isEmpty(childs) ? (
                            <Button
                                className="ecc-silk-mapping__treenav--item-toggler"
                                iconName={
                                    expanded ? 'expand_more' : 'arrow_nextpage'
                                }
                                tooltip={
                                    expanded ? 'Hide sub tree' : 'Open sub tree'
                                }
                                onClick={() => {
                                    this.handleToggleExpanded(id);
                                }}
                            />
                        ) : (
                            <Icon
                                className="ecc-silk-mapping__treenav--item-toggler"
                                name="radio_button_unchecked"
                                tooltip=""
                            />
                        )}
                        {element()}
                    </div>
                    {expanded ? (
                        <ul className="ecc-silk-mapping__treenav--subtree">
                            {_.map(childs, child => (
                                <li key={child.id}>
                                    {navigationList({parent: child})}
                                </li>
                            ))}
                        </ul>
                    ) : (
                        false
                    )}
                </div>
            );
        };

        const content = !_.isEmpty(tree) ? (
            <ul className="ecc-silk-mapping__treenav--maintree">
                <li>{navigationList({parent: tree})}</li>
            </ul>
        ) : (
            false
        );

        const loading = this.state.loading ? <Spinner /> : false;

        return (
            <div className="ecc-silk-mapping__treenav">
                <Card>
                    <CardContent>
                        {loading}
                        {content}
                    </CardContent>
                </Card>
            </div>
        );
    },

    renderRuleIcon(ruleId) {
        if(!this.props.ruleValidation || !this.props.ruleValidation.hasOwnProperty(ruleId)) {
            return (null);
        } else if(this.props.ruleValidation[ruleId] === "ok") {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-green" name="done" />
        } else {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-red" name="warning" />
        }
    }
});

export default MappingsTree;
