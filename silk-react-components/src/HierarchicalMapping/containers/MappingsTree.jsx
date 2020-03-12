import React from 'react';
import PropTypes from 'prop-types';
import _ from 'lodash';
import {
    Spinner,
    Button,
    Icon,
    Card,
    CardContent,
    Info,
} from '@eccenca/gui-elements';

import RuleTypes from '../elements/RuleTypes';
import RuleTitle from '../elements/RuleTitle';
import { MAPPING_RULE_TYPE_ROOT } from '../utils/constants';
import { getHierarchyAsync } from '../store';
import EventEmitter from '../utils/EventEmitter';
import { MAPPING_RULE_TYPE_OBJECT, MESSAGES } from '../utils/constants';

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
        EventEmitter.on(MESSAGES.RELOAD, this.loadNavigationTree);

        this.loadNavigationTree();
    }

    componentDidUpdate(prevProps, prevState, snapshot) {
        if (prevProps.currentRuleId !== this.props.currentRuleId) {
            this.expandNavigationTreeElement();
        }
    }

    componentWillUnmount() {
        EventEmitter.off(MESSAGES.RELOAD, this.loadNavigationTree);
    }

    loadNavigationTree = (args = {}) => {
        const { navigationExpanded } = this.state;
        this.setState({
            navigationLoading: true,
        });

        getHierarchyAsync()
            .subscribe(
                ({ hierarchy }) => {
                    const topLevelId = hierarchy.id;
                    this.setState({
                        navigationLoading: false,
                        data: hierarchy,
                        navigationExpanded: (_.isEmpty(navigationExpanded) && topLevelId)
                            ? { [topLevelId]: true }
                            : navigationExpanded,
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
                }
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
        const expanded = {
            ...this.state.navigationExpanded,
        };
        expanded[this.props.currentRuleId] = true;
        // @NOTE: we should pass the parent id by another way
        // expanded[parentId] = true;

        this.setState({
            navigationExpanded: expanded,
        });
    }

    /**
     * Returns an object which contains a key for each rule
     * that should be expanded because it contains a child with a warning.
     * @param originTreeElement The rule tree
     */
    markTree = originTreeElement => {
        if (_.isEmpty(originTreeElement)) {
            return originTreeElement;
        }

        const tree = _.cloneDeep(originTreeElement);

        const { id, type } = tree;

        let expanded = this.state.navigationExpanded[id] || false;
        let isHighlighted =
            id === this.state.currentRuleId ||
            (_.get(tree, 'rules.uriRule.id') === this.state.currentRuleId &&
                !_.isUndefined(this.state.currentRuleId)) ||
            (type === MAPPING_RULE_TYPE_ROOT &&
                _.isUndefined(this.state.currentRuleId));

        if (_.has(tree, 'rules.propertyRules')) {
            tree.rules.propertyRules = _.map(tree.rules.propertyRules, rule => {
                const subtree = this.markTree(rule);

                if (
                    subtree.type !== MAPPING_RULE_TYPE_OBJECT &&
                    subtree.id === this.state.currentRuleId
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
    };

    // construct parent-child tree
    navigationList = parent => {
        const {
            id,
            type: parentType,
            rules = {},
            isHighlighted,
            expanded,
        } = parent;

        const { showValueMappings, handleRuleNavigation } = this.props;

        // get expanded state
        const childs = _.chain(rules.propertyRules)
            .filter(({ type }) => showValueMappings || type === MAPPING_RULE_TYPE_OBJECT)
            .value();

        const element = () => (
            <button
                className="ecc-silk-mapping__treenav--item-handler"
                data-test-id={`ecc-silk-mapping__treenav__button-${id}`}
                onClick={() => { handleRuleNavigation({ newRuleId: id, parentId: undefined }); }}
            >
                <span className="ecc-silk-mapping__treenav--item-maintitle">
                    <span>
                        <RuleTitle rule={parent} />
                        { this.renderRuleIcon(id) }
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
                        isHighlighted ? ' ecc-silk-mapping__treenav--item-active' : ''
                    }`}
                >
                    {!_.isEmpty(childs) ? (
                        <Button
                            data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                            className="ecc-silk-mapping__treenav--item-toggler"
                            iconName={expanded ? 'expand_more' : 'arrow_nextpage'}
                            tooltip={expanded ? 'Hide sub tree' : 'Open sub tree'}
                            onClick={() => { this.handleToggleExpandNavigationTree(id); }}
                        />
                    ) : (
                        <Icon
                            data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                            className="ecc-silk-mapping__treenav--item-toggler"
                            name="radio_button_unchecked"
                            tooltip=""
                        />
                    )}
                    {element()}
                </div>
                {expanded && (
                    <ul className="ecc-silk-mapping__treenav--subtree">
                        {_.map(childs, child => (
                            <li key={child.id}>
                                {this.navigationList(child)}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        );
    };

    renderRuleIcon(ruleId) {
        if(!this.props.ruleValidation || !this.props.ruleValidation.hasOwnProperty(ruleId)) {
            return (null);
        } else if(this.props.ruleValidation[ruleId] === "ok") {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-green" name="done" />
        } else {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-red" name="warning" />
        }
    };

    render() {
        const { data, navigationLoading } = this.state;

        const tree = this.markTree(data);
        const NavigationList = this.navigationList(tree);

        return (
            <div className="ecc-silk-mapping__treenav">
                <Card>
                    <CardContent>
                        {navigationLoading && <Spinner />}
                        {navigationLoading && _.isUndefined(data) && (
                            <Info data-test-id="ecc-silk-mapping__treenav-loading">Loading rules</Info>
                        )}
                        {!navigationLoading && _.isEmpty(data) && (
                            <Info data-test-id="ecc-silk-mapping__treenav-norules">No rules found</Info>
                        )}
                        {
                            !_.isEmpty(data) && (
                                <ul className="ecc-silk-mapping__treenav--maintree">
                                    <li>
                                        {NavigationList}
                                    </li>
                                </ul>
                            )
                        }
                    </CardContent>
                </Card>
            </div>
        );
    }
}

MappingsTree.propTypes = {
    currentRuleId: PropTypes.string,
    handleRuleNavigation: PropTypes.func,
    showValueMappings: PropTypes.bool,
    // For each rule id, contains one of the following: "ok", "warning"
    ruleValidation: PropTypes.objectOf(React.PropTypes.oneOf(['ok', 'warning']))
};

MappingsTree.defaultProps = {
    currentRuleId: undefined,
    handleRuleNavigation: () => {},
};

export default MappingsTree;
