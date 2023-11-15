import React from "react";
import _ from "lodash";
import { Icon } from "gui-elements-deprecated";
import {
    InteractionGate,
    Notification,
    Tree,
    TreeNodeInfo,
    Button,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
    OverflowText,
} from "@eccenca/gui-elements";

import RuleTypes from "../elements/RuleTypes";
import RuleTitle from "../elements/RuleTitle";
import { MAPPING_RULE_TYPE_ROOT } from "../utils/constants";
import { getApiDetails, getHierarchyAsync, getRuleAsync } from "../store";
import EventEmitter from "../utils/EventEmitter";
import { MAPPING_RULE_TYPE_OBJECT, MESSAGES } from "../utils/constants";
import { getHistory } from "../../../../../store/configureStore";
import { useTranslation } from "react-i18next";

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

//React.useState<Map<string, boolean>>(new Map());

/** Tree structure of nested object mapping rules of a transform task. */
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
    const [treeNodes, setTreeNodes] = React.useState<TreeNodeInfo[]>([]);
    const [data, setData] = React.useState();
    const [t] = useTranslation();

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
    }, [data, currentRuleId]);

    const handleNodeExpand = React.useCallback(
        (node) => {
            setTreeNodes([buildTree(data, { nodeId: node.id, expanded: true })]);
        },
        [data, currentRuleId]
    );

    const handleNodeCollapse = React.useCallback(
        (node) => {
            setTreeNodes([buildTree(data, { nodeId: node.id, expanded: false })]);
        },
        [data, currentRuleId]
    );

    const renderRuleIcon = (ruleId) => {
        if (!ruleValidation || ruleValidation[ruleId] === undefined) {
            return null;
        } else if (ruleValidation[ruleId] === "ok") {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-green" name="done" />;
        } else {
            return <Icon className="ecc-silk-mapping__ruleitem-icon-yellow" name="warning" />;
        }
    };

    const buildTree = React.useCallback(
        (parent, config?: { nodeId: string; expanded: boolean }): TreeNodeInfo => {
            const { id, type: parentType, rules = {} } = parent;

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
                    className={`ecc-silk-mapping__treenav--item ecc-silk-mapping__treenav--item--ignorestyles${
                        currentRuleId === id ? " ecc-silk-mapping__treenav--item-active" : ""
                    }`}
                >
                    <Button
                        alignText="left"
                        minimal
                        fill
                        active={currentRuleId === id}
                        icon={renderRuleIcon(id) ?? undefined}
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
                        <OverviewItem densityHigh>
                            <OverviewItemDescription>
                                <OverviewItemLine className="ecc-silk-mapping__treenav--item-maintitle">
                                    <RuleTitle rule={parent} />
                                </OverviewItemLine>
                                {parentType === MAPPING_RULE_TYPE_OBJECT && (
                                    <OverviewItemLine small className="ecc-silk-mapping__treenav--item-rule-subtitle">
                                        <OverflowText>
                                            <RuleTypes rule={parent} />
                                        </OverflowText>
                                    </OverviewItemLine>
                                )}
                            </OverviewItemDescription>
                        </OverviewItem>
                    </Button>
                </div>
            );

            let isExpanded =
                config && config.nodeId === id
                    ? config.expanded
                    : currentRuleId === id
                    ? true
                    : parentType === MAPPING_RULE_TYPE_ROOT;

            const tree = {
                id,
                hasCaret: !!childNodes.length,
                isExpanded,
                label,
                icon: !childNodes.length ? (
                    <Icon
                        name="radio_button_unchecked"
                        data-test-id={`ecc-silk-mapping__treenav--item-toggler-${id}`}
                        className="ecc-silk-mapping__treenav--item-toggler"
                    />
                ) : undefined,
                childNodes: childNodes.map((child) => {
                    const subtree = buildTree(child, config);
                    if (!config && subtree.isExpanded) {
                        isExpanded = true;
                    }
                    return subtree;
                }),
            };
            tree.isExpanded = isExpanded;
            return tree;
        },
        [currentRuleId]
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
            <InteractionGate
                inert={navigationLoading}
                showSpinner={navigationLoading}
                spinnerProps={{ position: "inline", size: "small", delay: 50 }}
            >
                {navigationLoading && _.isUndefined(data) && (
                    <Notification neutral data-test-id="ecc-silk-mapping__treenav-loading">
                        {t("MappingTree.loadingRules")}
                    </Notification>
                )}
                {!navigationLoading && _.isEmpty(data) && (
                    <Notification warning data-test-id="ecc-silk-mapping__treenav-norules">
                        {t("MappingTree.noRulesFound")}
                    </Notification>
                )}
                {!_.isEmpty(data) && (
                    <Tree
                        className="ecc-silk-mapping__treenav--maintree"
                        contents={treeNodes}
                        onNodeExpand={handleNodeExpand}
                        onNodeCollapse={handleNodeCollapse}
                    />
                )}
            </InteractionGate>
        </div>
    );
};

export default MappingsTreeNew;
