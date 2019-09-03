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
import { MAPPING_RULE_TYPE_OBJECT, MAPPING_RULE_TYPE_ROOT } from '../helpers';

/**
 * Navigation tree of all mappings
 * @param props
 */
const MappingsTree = props => {
    const {
        currentRuleId, navigationTree, navigationExpanded, navigationLoading, showValueMappings,
        handleToggleExpanded,
        handleRuleNavigation
    } = props;
    /**
     * Returns an object which contains a key for each rule
     * that should be expanded because it contains a child with a warning.
     * @param tree The rule tree
     */
    const markTree = originTreeElement => {
        if (_.isEmpty(originTreeElement)) {
            return originTreeElement;
        }

        const tree = _.cloneDeep(originTreeElement);

        const { id, type } = tree;

        let expanded = navigationExpanded[id] || false;
        let isHighlighted =
            id === currentRuleId ||
            (_.get(tree, 'rules.uriRule.id') === currentRuleId &&
                !_.isUndefined(currentRuleId)) ||
            (type === MAPPING_RULE_TYPE_ROOT &&
                _.isUndefined(currentRuleId));

        if (_.has(tree, 'rules.propertyRules')) {
            tree.rules.propertyRules = _.map(tree.rules.propertyRules, rule => {
                const subtree = markTree(rule);

                if (
                    subtree.type !== MAPPING_RULE_TYPE_OBJECT &&
                    subtree.id === currentRuleId
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
    const navigationList = ({ parent }) => {
        const {
            id,
            type: parentType,
            rules = {},
            isHighlighted,
            expanded,
        } = parent;

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
                            onClick={() => { handleToggleExpanded(id); }}
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
                                {navigationList({ parent: child })}
                            </li>
                        ))}
                    </ul>
                )}
            </div>
        );
    };

    return (
        <div className="ecc-silk-mapping__treenav">
            <Card>
                <CardContent>
                    {navigationLoading && <Spinner />}
                    {navigationLoading && _.isUndefined(navigationTree) && (
                        <Info data-test-id="ecc-silk-mapping__treenav-loading">Loading rules</Info>
                    )}
                    {!navigationLoading && _.isEmpty(navigationTree) && (
                        <Info data-test-id="ecc-silk-mapping__treenav-norules">No rules found</Info>
                    )}
                    {
                        !_.isEmpty(navigationTree) && (
                            <ul className="ecc-silk-mapping__treenav--maintree">
                                <li>
                                    {navigationList({ parent: markTree(navigationTree) })}
                                </li>
                            </ul>
                        )
                    }
                </CardContent>
            </Card>
        </div>
    );
};

MappingsTree.propTypes = {
    // selected rule id (tree highlighting)
    currentRuleId: PropTypes.string,
    handleRuleNavigation: PropTypes.func,
    handleToggleExpanded: PropTypes.func,
    navigationTree: PropTypes.object,
    navigationExpanded: PropTypes.objectOf(PropTypes.bool.isRequired),
    navigationLoading: PropTypes.bool.isRequired,
    showValueMappings: PropTypes.bool,
};

MappingsTree.defaultProps = {
    navigationTree: undefined,
    currentRuleId: undefined,
    navigationExpanded: {},
    handleToggleExpanded: () => {},
    handleRuleNavigation: () => {}
};

export default MappingsTree;
