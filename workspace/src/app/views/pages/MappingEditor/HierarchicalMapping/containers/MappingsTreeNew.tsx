import React from "react";
import _ from "lodash";
import { Info, Icon } from "gui-elements-deprecated";
import { Card, CardContent, Spinner, Tree, TreeNodeInfo } from "@eccenca/gui-elements";

import RuleTypes from "../elements/RuleTypes";
import RuleTitle from "../elements/RuleTitle";
import { MAPPING_RULE_TYPE_ROOT } from "../utils/constants";
import { getApiDetails, getHierarchyAsync, getRuleAsync } from "../store";
import EventEmitter from "../utils/EventEmitter";
import { MAPPING_RULE_TYPE_OBJECT, MESSAGES } from "../utils/constants";
import { getHistory } from "../../../../../store/configureStore";

interface MappingTreeProps {
    currentRuleId: string;
    /*
     The mapping rule tree. Optional,
     because old components don't set this and rely on the message bus instead...
     */
    ruleTree?: any; //Todo change to correct type
    handleRuleNavigation: (...args) => void;
    trackRuleInUrl?: boolean;
    ruleValidation?: Record<string, "ok" | "warning">;
    showValueMappings?: boolean;
    startFullScreen?: boolean;
}

const MappingsTreeNew: React.FC<MappingTreeProps> = ({
    ruleTree,
    currentRuleId,
    trackRuleInUrl,
    handleRuleNavigation,
    ruleValidation,
    showValueMappings,
    startFullScreen,
}) => {
    const [navigationLoading, setNavigationLoading] = React.useState<boolean>(false);
    const [treeExpansionMap, setTreeExpansionMap] = React.useState<Map<string, boolean>>(new Map());
    const [currentlySelectedId, setCurrentlySelected] = React.useState<string>(MAPPING_RULE_TYPE_ROOT);
    const [treeNodes, setTreeNodes] = React.useState<TreeNodeInfo[]>([]);
    const [data, setData] = React.useState();

    React.useEffect(() => {
        updateNavigationTree();
        if (trackRuleInUrl) {
            // Ignore rule ID parameter in URL
            const searchQuery = new URLSearchParams(window.location.search).get("ruleId");
            if (searchQuery) {
                getRuleById(searchQuery);
            }
        }
        EventEmitter.on(MESSAGES.RELOAD, updateNavigationTree);

        return () => {
            if (ruleTree == null) {
                EventEmitter.off(MESSAGES.RELOAD, updateNavigationTree);
            }
        };
    }, []);

    React.useEffect(() => {
        updateNavigationTree();
    }, [ruleTree, currentRuleId]);

    React.useEffect(() => {
        if (data) {
            setTreeNodes([buildTree(data)]);
        }
    }, [data, treeExpansionMap, currentlySelectedId]);

    const handleNodeExpand = React.useCallback((node) => {
        setTreeExpansionMap((prev) => new Map([...prev, [node.id, true]]));
    }, []);

    const handleNodeCollapse = React.useCallback((node) => {
        setTreeExpansionMap((prev) => new Map([...prev, [node.id, false]]));
    }, []);

    const handleNodeClick = React.useCallback((node) => {
        setCurrentlySelected(node.id);
    }, []);

    const renderRuleIcon = (ruleId) => {
        if (!ruleValidation || !ruleValidation.hasOwnProperty(ruleId)) {
            return null;
        } else if (ruleValidation[ruleId] === "ok") {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-green" name="done" />;
        } else {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-yellow" name="warning" />;
        }
    };

    const buildTree = React.useCallback(
        (parent): TreeNodeInfo => {
            const { id, type: parentType, rules } = parent;

            let allRules = [] as any[];
            if (rules.uriRule != null) {
                allRules = allRules.concat([rules.uriRule]);
            }
            if (rules.propertyRules != null) {
                allRules = allRules.concat(rules.propertyRules);
            }

            const childNodes = allRules.filter(({ type }) => showValueMappings || type === MAPPING_RULE_TYPE_OBJECT);

            const label = (
                <div
                    className={`ecc-silk-mapping__treenav--item${
                        currentlySelectedId === id ? " ecc-silk-mapping__treenav--item-active" : ""
                    }`}
                >
                    <button
                        className="ecc-silk-mapping__treenav--item-handler"
                        data-test-id={`ecc-silk-mapping__treenav__button-${id}`}
                        onClick={() => {
                            if (!startFullScreen && trackRuleInUrl) {
                                const history = getHistory();
                                history.replace({
                                    search: `?${new URLSearchParams({ ruleId: id })}`,
                                });
                            }
                            handleRuleNavigation({ newRuleId: id, parentId: undefined });
                        }}
                    >
                        <span className="ecc-silk-mapping__treenav--item-maintitle">
                            <span>
                                <RuleTitle rule={parent} />
                                {renderRuleIcon(id)}
                            </span>
                        </span>
                        {parentType === MAPPING_RULE_TYPE_OBJECT && (
                            <small className="ecc-silk-mapping__treenav--item-rule-subtitle">
                                <RuleTypes rule={parent} />
                            </small>
                        )}
                    </button>
                </div>
            );
            return {
                id,
                hasCaret: !!childNodes.length,
                isExpanded: treeExpansionMap.get(id) ?? parentType === MAPPING_RULE_TYPE_ROOT,
                label,
                icon: !childNodes.length ? (
                    <Icon
                        name="radio_button_unchecked"
                        data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                        className="ecc-silk-mapping__treenav--item-toggler"
                    />
                ) : undefined,
                childNodes: childNodes.map((child) => buildTree(child)),
            };
        },
        [treeExpansionMap, currentlySelectedId]
    );

    const getRuleById = (searchId) => {
        if (!getApiDetails().transformTask) {
            // API details not loaded, do not continue
            return;
        }
        setNavigationLoading(true);
        getRuleAsync(searchId, true).subscribe(
            ({ rule }) => {
                handleRuleNavigation({ newRuleId: searchId });
            },
            () => {
                setNavigationLoading(false);
            }
        );
    };

    const updateNavigationTree = React.useCallback(
        (args = {}) => {
            if (ruleTree == null) {
                // The mapping rule has not been provided in the props, so it has to be loaded
                loadNavigationTree();
            } else {
                setNavigationLoading(false);
                setData(ruleTree);
            }
        },
        [ruleTree]
    );

    const loadNavigationTree = (args: Record<string, any> = {}) => {
        setNavigationLoading(true);
        getHierarchyAsync().subscribe(
            ({ hierarchy }) => {
                setNavigationLoading(false);
                setData(hierarchy);
                // setNavigationExpanded(updateExpandedRules(hierarchy, navigationExpanded));
            },
            () => {
                setNavigationLoading(false);
            },
            () => {
                if (args.onFinish) {
                    args.onFinish();
                }
            }
        );
    };

    return (
        <div className="ecc-silk-mapping__treenav">
            <Card isOnlyLayout={true}>
                <CardContent>
                    {navigationLoading && <Spinner position={"global"} />}
                    {navigationLoading && _.isUndefined(data) && (
                        <Info data-test-id="ecc-silk-mapping__treenav-loading">Loading rules</Info>
                    )}
                    {!navigationLoading && _.isEmpty(data) && (
                        <Info data-test-id="ecc-silk-mapping__treenav-norules">No rules found</Info>
                    )}
                    {!_.isEmpty(data) && (
                        <Tree
                            className="ecc-silk-mapping__treenav--maintree"
                            contents={treeNodes}
                            onNodeExpand={handleNodeExpand}
                            onNodeCollapse={handleNodeCollapse}
                            onNodeClick={handleNodeClick}
                        />
                    )}
                </CardContent>
            </Card>
        </div>
    );
};

export default MappingsTreeNew;
