import React from "react";
import PropTypes from "prop-types";
import _ from "lodash";
import { InteractionGate, Notification, Icon, IconButton } from "@eccenca/gui-elements";

import RuleTypes from "../elements/RuleTypes";
import RuleTitle from "../elements/RuleTitle";
import { MAPPING_RULE_TYPE_ROOT } from "../utils/constants";
import { getApiDetails, getHierarchyAsync, getRuleAsync } from "../store";
import EventEmitter from "../utils/EventEmitter";
import { MAPPING_RULE_TYPE_OBJECT, MESSAGES } from "../utils/constants";
import { getHistory } from "../../../../../store/configureStore";

/**
 * Navigation tree of all mappings
 * @param props
 */
class MappingsTree extends React.Component {
    state = {
        navigationLoading: false,
        navigationExpanded: {},
        data: {},
    };

    componentDidMount() {
        this.updateNavigationTree();
        if (this.props.trackRuleInUrl) {
            // Ignore rule ID parameter in URL
            const searchQuery = new URLSearchParams(window.location.search).get("ruleId");
            if (searchQuery) {
                this.getRuleById(searchQuery);
            }
        }
        EventEmitter.on(MESSAGES.RELOAD, this.updateNavigationTree);
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (prevProps.ruleTree !== this.props.ruleTree) {
            this.updateNavigationTree();
        } else if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.expandNavigationTreeElement();
        }
    }

    componentWillUnmount() {
        if (this.props.ruleTree == null) {
            EventEmitter.off(MESSAGES.RELOAD, this.updateNavigationTree);
        }
    }

    getRuleById = (searchId) => {
        if (!getApiDetails().transformTask) {
            // API details not loaded, do not continue
            return;
        }
        this.setState({
            navigationLoading: true,
        });
        getRuleAsync(searchId, true).subscribe(
            ({ rule }) => {
                const navigationExpanded = {
                    ...rule.breadcrumbs.reduce((exp, breadcrumb) => {
                        exp[breadcrumb.id] = true;
                        return exp;
                    }, {}),
                    [rule.id]: true,
                };
                this.props.handleRuleNavigation({ newRuleId: searchId, containerRuleId: rule.id });
                this.setState({ navigationExpanded });
            },
            () => {
                this.setState({ navigationLoading: false });
            },
        );
    };

    updateNavigationTree = (args = {}) => {
        if (this.props.ruleTree == null) {
            // The mapping rule has not been provided in the props, so it has to be loaded
            this.loadNavigationTree();
        } else {
            this.setState({
                navigationLoading: false,
                data: this.props.ruleTree,
                navigationExpanded: this.updateExpandedRules(this.props.ruleTree, this.state.navigationExpanded),
            });
        }
    };

    loadNavigationTree = (args = {}) => {
        const { navigationExpanded } = this.state;
        this.setState({
            navigationLoading: true,
        });
        getHierarchyAsync().subscribe(
            ({ hierarchy }) => {
                this.setState({
                    navigationLoading: false,
                    data: hierarchy,
                    navigationExpanded: this.updateExpandedRules(hierarchy, navigationExpanded),
                });
            },
            () => {
                this.setState({
                    navigationLoading: false,
                });
            },
            () => {
                if (args.onFinish) {
                    args.onFinish();
                }
            },
        );
    };

    // collapse / expand navigation children
    handleToggleExpandNavigationTree(id) {
        const expanded = {
            ...this.state.navigationExpanded,
        };
        expanded[id] = !expanded[id];
        this.setState({
            navigationExpanded: expanded,
        });
    }

    expandNavigationTreeElement() {
        const expanded = this.updateExpandedRules(this.state.data, this.state.navigationExpanded);
        this.setState({
            navigationExpanded: expanded,
        });
    }

    // Updates the expanded rules list based on the currently selected rule
    updateExpandedRules(tree, currentExpanded) {
        const expanded = {
            ...currentExpanded,
        };
        if (this.props.currentRuleId) {
            expanded[this.props.currentRuleId] = true;
        } else {
            expanded[tree.id] = true; // Expand root rule
        }
        // also expand all parent nodes
        const parentRuleIds = MappingsTree.extractParentIds(tree, this.props.currentRuleId);
        _.forEach(parentRuleIds, (ruleId) => {
            expanded[ruleId] = true;
        });
        return expanded;
    }

    // Extracts the parent IDs of the currently selected rule
    static extractParentIds = (tree, currentId) => {
        if (tree.id === currentId) {
            return [currentId];
        } else {
            if (_.has(tree, "rules.propertyRules")) {
                const objectRules = _.filter(tree.rules.propertyRules, (rule) => {
                    return rule.type === "object";
                });
                const objectRuleResults = _.map(objectRules, (rule) => {
                    return MappingsTree.extractParentIds(rule, currentId);
                });
                const nonEmptyResult = _.find(objectRuleResults, (result) => {
                    return result.length > 0;
                });
                if (nonEmptyResult === undefined) {
                    return [];
                } else {
                    nonEmptyResult.unshift(tree.id);
                    return _.filter(nonEmptyResult, (ruleId) => {
                        return ruleId !== currentId;
                    });
                }
            } else {
                return [];
            }
        }
    };

    /**
     * Returns an object which contains a key for each rule
     * that should be expanded because it contains a child with a warning.
     * @param originTreeElement The rule tree
     */
    markTree = (originTreeElement) => {
        if (_.isEmpty(originTreeElement)) {
            return originTreeElement;
        }

        const tree = _.cloneDeep(originTreeElement);

        const { id, type } = tree;

        let expanded = this.state.navigationExpanded[id] || false;

        let isHighlighted =
            id === this.props.currentRuleId ||
            (_.get(tree, "rules.uriRule.id") === this.props.currentRuleId &&
                !_.isUndefined(this.props.currentRuleId)) ||
            (type === MAPPING_RULE_TYPE_ROOT && _.isUndefined(this.props.currentRuleId));

        if (_.has(tree, "rules.propertyRules")) {
            tree.rules.propertyRules = _.map(tree.rules.propertyRules, (rule) => {
                const subtree = this.markTree(rule);

                if (subtree.type !== MAPPING_RULE_TYPE_OBJECT && subtree.id === this.props.currentRuleId) {
                    isHighlighted = true;
                    expanded = true;
                }

                return subtree;
            });
        }

        tree.expanded = expanded;
        tree.isHighlighted = isHighlighted;

        return tree;
    };

    // construct parent-child tree
    navigationList = (parent) => {
        const { id, type: parentType, rules = {}, isHighlighted, expanded } = parent;

        const { showValueMappings, handleRuleNavigation } = this.props;
        var allRules = [];
        if (rules.uriRule != null) {
            allRules = allRules.concat([rules.uriRule]);
        }
        if (rules.propertyRules != null) {
            allRules = allRules.concat(rules.propertyRules);
        }

        // get expanded state
        const childs = _.chain(allRules)
            .filter(({ type }) => showValueMappings || type === MAPPING_RULE_TYPE_OBJECT)
            .value();
        const element = () => (
            <button
                className="ecc-silk-mapping__treenav--item-handler"
                data-test-id={`ecc-silk-mapping__treenav__button-${id}`}
                onClick={() => {
                    if (!this.props.startFullScreen && this.props.trackRuleInUrl) {
                        const history = getHistory();
                        history.replace({
                            search: `?${new URLSearchParams({ ruleId: id })}`,
                        });
                    }
                    handleRuleNavigation({ newRuleId: id, parentId: undefined, containerRuleId: id });
                }}
            >
                <span className="ecc-silk-mapping__treenav--item-maintitle">
                    <span>
                        {this.renderRuleIcon(id)}
                        <RuleTitle rule={parent} />
                    </span>
                </span>
                {parentType === MAPPING_RULE_TYPE_OBJECT && (
                    <small className="ecc-silk-mapping__treenav--item-subtitle">
                        <RuleTypes rule={parent} />
                    </small>
                )}
            </button>
        );

        return (
            <div>
                <div
                    className={`ecc-silk-mapping__treenav--item${
                        isHighlighted ? " ecc-silk-mapping__treenav--item-active" : ""
                    }`}
                >
                    {!_.isEmpty(childs) ? (
                        <IconButton
                            data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                            className="ecc-silk-mapping__treenav--item-toggler"
                            name={expanded ? "toggler-showmore" : "toggler-moveright"}
                            tooltip={expanded ? "Hide sub tree" : "Open sub tree"}
                            onClick={() => {
                                this.handleToggleExpandNavigationTree(id);
                            }}
                        />
                    ) : (
                        <IconButton
                            data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                            className="ecc-silk-mapping__treenav--item-toggler"
                            name="toggler-radio"
                            tooltip=""
                            disabled
                            small
                        />
                    )}
                    {element()}
                </div>
                {expanded && (
                    <ul className="ecc-silk-mapping__treenav--subtree">
                        {_.map(childs, (child) => (
                            <li key={child.id}>{this.navigationList(child)}</li>
                        ))}
                    </ul>
                )}
            </div>
        );
    };

    renderRuleIcon(ruleId) {
        if (!this.props.ruleValidation || !this.props.ruleValidation.hasOwnProperty(ruleId)) {
            return null;
        } else if (this.props.ruleValidation[ruleId] === "ok") {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-green" name="state-success" intent="success" />;
        } else {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-yellow" name="state-warning" intent="warning" />;
        }
    }

    render() {
        const { data, navigationLoading } = this.state;
        const tree = this.markTree(data);
        const NavigationList = this.navigationList(tree);

        return (
            <div className="ecc-silk-mapping__treenav">
                <InteractionGate inert={navigationLoading} showSpinner={navigationLoading}>
                    {navigationLoading && _.isUndefined(data) && (
                        <Notification neutral data-test-id="ecc-silk-mapping__treenav-loading">
                            Loading rules
                        </Notification>
                    )}
                    {!navigationLoading && _.isEmpty(data) && (
                        <Notification info data-test-id="ecc-silk-mapping__treenav-norules">
                            No rules found
                        </Notification>
                    )}
                    {!_.isEmpty(data) && (
                        <ul className="ecc-silk-mapping__treenav--maintree">
                            <li>{NavigationList}</li>
                        </ul>
                    )}
                </InteractionGate>
            </div>
        );
    }
}

MappingsTree.propTypes = {
    currentRuleId: PropTypes.string,
    ruleTree: PropTypes.object,
    handleRuleNavigation: PropTypes.func,
    showValueMappings: PropTypes.bool,
    // For each rule id, contains one of the following: "ok", "warning"
    ruleValidation: PropTypes.objectOf(PropTypes.oneOf(["ok", "warning"])),
    trackRuleInUrl: PropTypes.bool,
};

MappingsTree.defaultProps = {
    currentRuleId: undefined,
    ruleTree: undefined, // The mapping rule tree. Optional, because old components don't set this and rely on the message bus instead...
    handleRuleNavigation: () => {},
    trackRuleInUrl: true,
};

export default MappingsTree;
