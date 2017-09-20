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
} from 'ecc-gui-elements';

import UseMessageBus from '../UseMessageBusMixin';
import hierarchicalMappingChannel from '../store';
import {RuleTreeTitle, RuleTreeTypes} from './MappingRule/SharedComponents';
import {MAPPING_RULE_TYPE_OBJECT, MAPPING_RULE_TYPE_ROOT} from '../helpers';

const MappingsTree = React.createClass({
    mixins: [UseMessageBus],

    // define property types
    propTypes: {
        currentRuleId: React.PropTypes.string, // currently selected rule id (tree highlighting)
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
    expandElement({parent}) {
        if (
            !_.isUndefined(parent) &&
            !_.includes(this.state.expanded, {[parent]: true})
        ) {
            this.setState({
                expanded: _.merge(this.state.expanded, {[parent]: true}),
            });
        }
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
                            ? {[topLevelId]: true}
                            : this.state.expanded,
                });
            },
            err => {
                console.warn('err MappingsTree: hierarchy.get', err);
                this.setState({loading: false});
            }
        );
    },
    // select clicked id
    handleNavigate(ruleId) {
        hierarchicalMappingChannel
            .subject('ruleId.change')
            .onNext({newRuleId: ruleId});
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
    markTree(curr) {
        if (_.isEmpty(curr)) {
            return curr;
        }

        const tree = _.cloneDeep(curr);

        const {id, type} = tree;

        let expanded = _.get(this.state, ['expanded', id], false);
        let isHighlighted =
            id === this.props.currentRuleId ||
            (type === MAPPING_RULE_TYPE_ROOT && _.isUndefined(this.props.currentRuleId));

        if (_.has(tree, 'rules.propertyRules')) {
            tree.rules.propertyRules = _.map(tree.rules.propertyRules, rule => {
                const subtree = this.markTree(rule);

                expanded = expanded || subtree.expanded;

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
                .filter(({type}) => type === MAPPING_RULE_TYPE_OBJECT)
                .value();

            const element = () =>
                <button
                    className="ecc-silk-mapping__treenav--item-handler"
                    onClick={() => {
                        this.handleNavigate(id);
                    }}>
                    <span className="ecc-silk-mapping__treenav--item-maintitle">
                        <RuleTreeTitle rule={parent} />
                    </span>
                    {parentType === MAPPING_RULE_TYPE_OBJECT
                        ? <small className="ecc-silk-mapping__treenav--item-subtitle">
                              {<RuleTreeTypes rule={parent} />}
                          </small>
                        : false}
                </button>;

            return (
                <div>
                    <div
                        className={`ecc-silk-mapping__treenav--item${isHighlighted
                            ? ' ecc-silk-mapping__treenav--item-active'
                            : ''}`}>
                        {!_.isEmpty(childs)
                            ? <Button
                                  className="ecc-silk-mapping__treenav--item-toggler"
                                  iconName={
                                      expanded
                                          ? 'expand_more'
                                          : 'arrow_nextpage'
                                  }
                                  tooltip={
                                      expanded
                                          ? 'Hide sub tree'
                                          : 'Open sub tree'
                                  }
                                  onClick={() => {
                                      this.handleToggleExpanded(id);
                                  }}
                              />
                            : <Icon
                                  className="ecc-silk-mapping__treenav--item-toggler"
                                  name="radio_button_unchecked"
                                  tooltip=""
                              />}
                        {element()}
                    </div>
                    {expanded
                        ? <ul className="ecc-silk-mapping__treenav--subtree">
                              {_.map(childs, child =>
                                  <li key={child.id}>
                                      {navigationList({parent: child})}
                                  </li>
                              )}
                          </ul>
                        : false}
                </div>
            );
        };

        const content = !_.isEmpty(tree)
            ? <ul className="ecc-silk-mapping__treenav--maintree">
                  <li>
                      {navigationList({parent: tree})}
                  </li>
              </ul>
            : false;

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
});

export default MappingsTree;
