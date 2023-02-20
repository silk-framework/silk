import {
    ActivityControlWidget,
    ConfidenceValue,
    ContextMenu,
    Divider,
    Highlighter,
    IActivityStatus,
    IconButton,
    MenuItem,
    Notification,
    OverflowText,
    SearchField,
    Spacing,
    Spinner,
    Switch,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableExpandedRow,
    TableExpandHeader,
    TableExpandRow,
    TableHead,
    TableHeader,
    TableRow,
    Tag,
    TagList,
    Toolbar,
    ToolbarSection,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../../views/shared/TaskActivityWidget/TaskActivityWidget";
import {
    getEvaluatedLinks,
    getLinkRuleInputPaths,
    getOperatorLabel,
    getParentNodes,
    updateReferenceLink,
} from "./LinkingEvaluationViewUtils";
import {
    EvaluationLinkInputValue,
    HoveredValuedType,
    LinkEvaluationFilters,
    LinkEvaluationSortBy,
    LinkEvaluationSortByObj,
    LinkRuleEvaluationResult,
    ReferenceLinkType,
} from "./typings";
import utils from "../LinkingRuleEvaluation.utils";
import { ComparisonDataCell, ComparisonDataContainer } from "../../activeLearning/components/ComparisionData";
import { ActiveLearningValueExamples } from "../../activeLearning/shared/ActiveLearningValueExamples";
import { PropertyBox } from "../../activeLearning/components/PropertyBox";
import { IAggregationOperator, IComparisonOperator } from "../../linking.types";
import { TreeNodeInfo } from "@blueprintjs/core";
import { EvaluationResultType } from "../LinkingRuleEvaluation";
import { tagColor } from "../../../../../views/shared/RuleEditor/view/sidebar/RuleOperator";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { debounce } from "lodash";
import { workspaceSel } from "@ducks/workspace";
import { useSelector } from "react-redux";
import { usePagination } from "@eccenca/gui-elements/src/components/Pagination/Pagination";
import { useFirstRender } from "../../../../../hooks/useFirstRender";
import TableTree from "./shared/TableTreeView";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const operatorInputMapping = {
    transformInput: "Transform",
    pathInput: "Input",
};

const linkStateButtons = [
    { icon: "state-confirmed", hasStateSuccess: true, linkType: "positive", tooltip: "Confirm" },
    { icon: "item-question", linkType: "unlabeled", tooltip: "Uncertain" },
    { icon: "state-declined", hasStateDanger: true, linkType: "negative", tooltip: "Decline" },
] as const;

const sortDirectionMapping = {
    NONE: "ASC",
    ASC: "DESC",
    DESC: "NONE",
} as const;

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const commonSel = useSelector(workspaceSel.commonSelector);
    const evaluationResults = React.useRef<LinkRuleEvaluationResult | undefined>();
    const [pagination, paginationElement, onTotalChange] = usePagination({
        pageSizes: [10, 25, 50, 100],
        initialPageSize: 25,
    });
    const [loading, setLoading] = React.useState<boolean>(false);
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<number, number>>(new Map());
    const [nodes, setNodes] = React.useState<TreeNodeInfo[]>([]);
    const linksToValueMap = React.useRef<Array<Map<string, EvaluationResultType[number]>>>([]);
    const [inputValuesExpansion, setInputValuesExpansion] = React.useState<
        Map<number, { expanded: boolean; precinct: boolean }>
    >(new Map());
    const [operatorsExpansion, setOperatorsExpansion] = React.useState<
        Map<number, { expanded: boolean; precinct: boolean }>
    >(new Map());
    const [tableValueQuery, setTableValueQuery] = React.useState<Map<number, HoveredValuedType>>(new Map());
    const [treeValueQuery, setTreeValueQuery] = React.useState<Map<number, HoveredValuedType>>(new Map());
    const [taskEvaluationStatus, setTaskEvaluationStatus] = React.useState<
        IActivityStatus["concreteStatus"] | undefined
    >();
    const [operatorPlugins, setOperatorPlugins] = React.useState<Array<IPluginDetails>>([]);
    const [nodeParentHighlightedIds, setNodeParentHighlightedIds] = React.useState<Map<number, string[]>>(new Map());
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    const [linkStateFilter, setLinkStateFilter] = React.useState<keyof typeof LinkEvaluationFilters>();
    const [linkSortBy, setLinkSortBy] = React.useState<Array<LinkEvaluationSortBy>>([]);
    const hasRenderedBefore = useFirstRender();
    const [tableSortDirection, setTableSortDirection] = React.useState<
        Map<typeof headerData[number]["key"], "ASC" | "DESC" | "NONE">
    >(
        () =>
            new Map([
                ["source", "NONE"],
                ["target", "NONE"],
                ["confidence", "NONE"],
            ])
    );

    //fetch operator plugins
    React.useEffect(() => {
        (async () => {
            setOperatorPlugins(Object.values((await requestRuleOperatorPluginDetails(false)).data));
        })();
    }, []);

    React.useEffect(() => {
        if (evaluationResults.current) {
            onTotalChange(evaluationResults.current.resultStats.filteredLinkCount);
        }
    }, [evaluationResults]);

    const getEvaluatedLinksUtil = React.useCallback(async (pagination, searchQuery = "", filters, linkSortBy) => {
        try {
            setLoading(true);
            const results = (
                await getEvaluatedLinks(projectId, linkingTaskId, pagination, searchQuery, filters, linkSortBy)
            )?.data;
            evaluationResults.current = results;
            linksToValueMap.current = results?.links.map((link) => utils.linkToValueMap(link as any)) ?? [];
            setInputValuesExpansion(
                () => new Map(results?.links.map((_, idx) => [idx, { expanded: showInputValues, precinct: true }]))
            );
            setOperatorsExpansion(
                () => new Map(results?.links.map((_, idx) => [idx, { expanded: showOperators, precinct: true }]))
            );
        } catch (err) {
        } finally {
            setLoading(false);
        }
    }, []);

    const debouncedSearchUtil = React.useCallback(
        debounce(async (searchQuery: string) => {
            await getEvaluatedLinksUtil(pagination, searchQuery, linkStateFilter ? [linkStateFilter] : [], linkSortBy);
        }, 500),
        []
    );

    React.useEffect(() => {
        let shouldCancel = false;
        if (!shouldCancel && hasRenderedBefore) {
            debouncedSearchUtil(searchQuery);
        }

        return () => {
            shouldCancel = true;
        };
    }, [searchQuery]);

    //initial loads of links
    React.useEffect(() => {
        let shouldCancel = false;
        if (!shouldCancel && taskEvaluationStatus === "Successful") {
            getEvaluatedLinksUtil(pagination, searchQuery, linkStateFilter ? [linkStateFilter] : [], linkSortBy);
        }
        return () => {
            shouldCancel = true;
        };
    }, [pagination.current, pagination.limit, taskEvaluationStatus, linkStateFilter, linkSortBy.length]);

    const handleAlwaysExpandSwitch = React.useCallback(
        (inputSwitch: "operator" | "inputValue") => {
            if (evaluationResults.current?.links.length) {
                //if expansion buttons have been triggered by user, maintain user's update while changing the default of the rest.
                const newUpdate = (prev) =>
                    new Map(
                        evaluationResults.current?.links.map((_: any, idx: number) => {
                            const { precinct, expanded } = prev.get(idx);
                            return [
                                idx,
                                {
                                    expanded: precinct ? !expanded : expanded,
                                    precinct: precinct ?? true,
                                },
                            ];
                        })
                    );
                inputSwitch === "inputValue" ? setInputValuesExpansion(newUpdate) : setOperatorsExpansion(newUpdate);
            }
        },
        [showOperators, showInputValues, evaluationResults]
    );

    const updateTreeNodes = React.useCallback(
        (idx: number) => {
            const operatorNode = evaluationResults.current?.linkRule.operator as any;
            if (!operatorNode) return;
            const treeInfo: TreeNodeInfo = {
                id: operatorNode.id,
                isExpanded: operatorsExpansion.get(idx)?.expanded ?? false,
                hasCaret: false,
                label: (
                    <span>
                        <Tag backgroundColor={tagColor(operatorNode.type)}>
                            {getOperatorLabel(operatorNode, operatorPlugins)}
                        </Tag>
                        <Spacing vertical size="tiny" />
                        {getOperatorConfidence(operatorNode.id, idx)}
                    </span>
                ),
                childNodes: [],
            };

            const getSubTree = (node: any, parentTree?: TreeNodeInfo) => {
                //Possibly comparison operators
                const inputPathCategory = {
                    sourceInput: "Source path",
                    targetInput: "Target path",
                };

                if (node?.inputs?.length) {
                    node.inputs.forEach((nodeInput) => {
                        getSubTree(nodeInput, {
                            id: nodeInput.id,
                            isExpanded: true,
                            hasCaret: false,
                            label: (
                                <span>
                                    <Tag backgroundColor={tagColor(nodeInput.type)}>
                                        {getOperatorLabel(nodeInput, operatorPlugins)}
                                    </Tag>
                                    <Spacing vertical size="tiny" />
                                    {getOperatorConfidence(nodeInput.id, idx)}
                                </span>
                            ),
                            childNodes: [],
                        });
                    });
                } else {
                    ["sourceInput", "targetInput"].forEach((inputPath) => {
                        const isSourceEntity = inputPath === "sourceInput";
                        //is comparison operator
                        let inputNode: TreeNodeInfo = {
                            id: node[inputPath].id,
                            isExpanded: true,
                            hasCaret: false,
                            label: (
                                <TagList>
                                    <Tag
                                        key="pathinput"
                                        backgroundColor={
                                            tagColor(
                                                node[inputPath].type === "pathInput"
                                                    ? inputPathCategory[inputPath]
                                                    : operatorInputMapping[node[inputPath].type]
                                            ) as string
                                        }
                                    >
                                        {getOperatorLabel(node[inputPath], operatorPlugins)}
                                    </Tag>
                                    {getLinkValues(node[inputPath].id, idx, treeInfo, {
                                        path: node[inputPath].path ?? "",
                                        isSourceEntity,
                                    })}
                                </TagList>
                            ),
                            childNodes: [],
                        };

                        if (node[inputPath].inputs?.length) {
                            node[inputPath].inputs.forEach((i) => {
                                buildInputTree(
                                    i,
                                    inputNode,
                                    idx,
                                    inputPathCategory[inputPath],
                                    treeInfo,
                                    isSourceEntity
                                );
                            });
                        }

                        if (parentTree) {
                            parentTree.childNodes!.push(inputNode);
                        } else {
                            treeInfo.childNodes!.push(inputNode);
                        }
                    });
                }
                parentTree && treeInfo.childNodes!.push(parentTree);
            };

            operatorNode.inputs?.length
                ? //Aggregation operator
                  (operatorNode as IAggregationOperator).inputs.forEach((i: any) => {
                      getSubTree(i, {
                          id: i.id,
                          isExpanded: true,
                          hasCaret: false,
                          label: (
                              <span>
                                  <Tag backgroundColor={tagColor(i.type)}>{getOperatorLabel(i, operatorPlugins)}</Tag>
                                  <Spacing vertical size="tiny" />
                                  {getOperatorConfidence(i.id, idx)}
                              </span>
                          ),
                          childNodes: [],
                      });
                  })
                : getSubTree(operatorNode);

            return treeInfo;
        },
        [
            evaluationResults.current,
            operatorPlugins.length,
            treeValueQuery,
            nodeParentHighlightedIds,
            operatorsExpansion,
        ]
    );

    React.useEffect(() => {
        const [changedRowIndex] = Array.from(treeValueQuery.keys());
        setNodes((prevNodes) =>
            prevNodes.map((prevNode, i) => (changedRowIndex === i ? { ...updateTreeNodes(i)! } : prevNode))
        );
    }, [treeValueQuery]);

    React.useEffect(() => {
        const [changedRowIndex] = Array.from(nodeParentHighlightedIds.keys());
        setNodes((prevNodes) =>
            prevNodes.map((prevNode, i) => (changedRowIndex === i ? { ...updateTreeNodes(i)! } : prevNode))
        );
    }, [nodeParentHighlightedIds]);

    React.useEffect(() => {
        if ((!evaluationResults.current || !operatorsExpansion.size) && hasRenderedBefore) return;
        setNodes(() =>
            new Array(evaluationResults.current?.links.length).fill(1).map((_, idx) => updateTreeNodes(idx)!)
        );
    }, [evaluationResults.current, operatorPlugins.length, operatorsExpansion]);

    React.useEffect(() => {
        if (
            evaluationResults.current &&
            evaluationResults.current.linkRule &&
            evaluationResults.current.links &&
            linksToValueMap.current.length
        ) {
            const ruleOperator = evaluationResults.current.linkRule.operator;
            if (ruleOperator) {
                const recursivelyGetInputPath = (operator: any): EvaluationLinkInputValue<string> => {
                    return ((operator as IAggregationOperator)?.inputs ?? []).reduce(
                        (acc, input) => {
                            if (!(input as any)?.inputs?.length) {
                                const linkRuleInputPaths = getLinkRuleInputPaths(input);
                                acc = {
                                    source: {
                                        ...acc.source,
                                        ...linkRuleInputPaths.source,
                                    },
                                    target: {
                                        ...acc.target,
                                        ...linkRuleInputPaths.target,
                                    },
                                };
                            } else {
                                const recursedAggregate = recursivelyGetInputPath(input);
                                acc = {
                                    source: {
                                        ...acc.source,
                                        ...recursedAggregate.source,
                                    },
                                    target: {
                                        ...acc.target,
                                        ...recursedAggregate.target,
                                    },
                                };
                            }

                            return acc;
                        },
                        { source: {}, target: {} } as EvaluationLinkInputValue<string>
                    );
                };

                const inputPaths = (ruleOperator as IComparisonOperator)?.sourceInput
                    ? getLinkRuleInputPaths(ruleOperator)
                    : recursivelyGetInputPath(ruleOperator);

                setInputValues(() =>
                    linksToValueMap.current.map((linkToValueMap) =>
                        ["source", "target"].reduce(
                            (matchingInputValue, inputPathType: "source" | "target") => {
                                Object.entries(inputPaths[inputPathType]).forEach(([uri, operatorId]) => {
                                    matchingInputValue[inputPathType][uri] =
                                        linkToValueMap.get(operatorId)?.value ?? [];
                                });
                                return matchingInputValue;
                            },
                            { source: {}, target: {} } as EvaluationLinkInputValue
                        )
                    )
                );
            }
        }
    }, [linksToValueMap.current]);

    const buildInputTree = (
        input: any,
        tree: TreeNodeInfo,
        index: number,
        tagInputTag: string,
        parentTree: TreeNodeInfo,
        isSourceEntity = false
    ): TreeNodeInfo => {
        if (!input.inputs?.length) {
            const newChild = {
                id: input.id,
                hasCaret: false,
                isExpanded: true,
                label: (
                    <TagList>
                        <Tag key="input" backgroundColor={tagColor(tagInputTag) as string}>
                            {getOperatorLabel(input, operatorPlugins)}
                        </Tag>
                        {getLinkValues(input.id, index, parentTree, {
                            path: input.path ?? "",
                            isSourceEntity,
                        })}
                    </TagList>
                ),
            };
            tree.childNodes = [...(tree?.childNodes ?? []), newChild];
            return tree;
        }

        return input.inputs.reduce((acc, i) => {
            const newChildTree = {
                id: input.id,
                hasCaret: false,
                isExpanded: true,
                label: (
                    <TagList>
                        <Tag key="operator" backgroundColor={tagColor(operatorInputMapping[input.type]) as string}>
                            {getOperatorLabel(input, operatorPlugins)}
                        </Tag>
                        {getLinkValues(input.id, index, parentTree, {
                            path: input.path ?? "",
                            isSourceEntity,
                        })}
                    </TagList>
                ),
            };
            tree.childNodes = [...(tree?.childNodes ?? []), newChildTree];

            acc = buildInputTree(i, newChildTree, index, tagInputTag, parentTree, isSourceEntity);
            return acc;
        }, {} as TreeNodeInfo);
    };

    const getOperatorConfidence = React.useCallback(
        (id: string, index: number) => {
            const linkToValueMap = linksToValueMap.current[index];
            if (!linksToValueMap.current.length || !linkToValueMap) return <></>;
            return linkToValueMap.get(id)?.value.map((val, i) => (
                <ConfidenceValue
                    key={i}
                    value={val.includes("Score") ? Number(val.replace("Score: ", "")) : Number(val)}
                    spaceUsage="minimal"
                    tagProps={{
                        // TODO: get color from CSS config
                        backgroundColor: nodeParentHighlightedIds.get(index)?.includes(id) ? "#0097a7" : undefined,
                    }}
                />
            ));
        },
        [linksToValueMap.current, nodeParentHighlightedIds, pagination]
    );

    const getLinkValues = React.useCallback(
        (id: string, index: number, tree: TreeNodeInfo, nodeData: Omit<HoveredValuedType, "value">) => {
            const cutAfter = 14;
            const linkToValueMap = linksToValueMap.current[index];
            if (linksToValueMap.current.length && linkToValueMap && nodeData) {
                const currentHighlightedValue = treeValueQuery.get(index);
                //if path === path from state and is sourceEntity matches and value matches
                const isHighlightMatch = (val: string) =>
                    nodeData &&
                    currentHighlightedValue &&
                    currentHighlightedValue.value === val &&
                    Object.entries(nodeData).reduce((acc, [key, val]) => {
                        acc = acc && currentHighlightedValue[key] === val;
                        return acc;
                    }, true);
                const otherCount =
                    (linkToValueMap.get(id)?.value || []).length > cutAfter ? (
                        <Tag className="diapp-linking-evaluation__cutinfo" round intent="info">
                            +{(linkToValueMap.get(id)?.value || []).length - cutAfter}
                        </Tag>
                    ) : (
                        <></>
                    );
                const exampleValues = linkToValueMap
                    .get(id)
                    ?.value.slice(0, cutAfter)
                    .map((val, i) => (
                        <Tag
                            key={val + i}
                            round
                            emphasis="stronger"
                            interactive
                            backgroundColor={
                                isHighlightMatch(val)
                                    ? "#746a85" // TODO: get color from CSS config
                                    : nodeParentHighlightedIds.get(index)?.includes(id)
                                    ? "#0097a7" // TODO: get color from CSS config
                                    : undefined
                            }
                            onMouseEnter={() => {
                                handleValueHover("tree", index, {
                                    value: val,
                                    ...nodeData,
                                });
                                handleParentNodeHighlights(tree, id, index);
                            }}
                            onMouseLeave={() => {
                                handleValueHover("tree", index, { value: "", path: "", isSourceEntity: false });
                                handleParentNodeHighlights(tree, id, index, true);
                            }}
                        >
                            {val}
                        </Tag>
                    ));
                return [exampleValues, [otherCount]];
            }
        },
        [linksToValueMap.current, treeValueQuery, nodeParentHighlightedIds]
    );

    const handleParentNodeHighlights = React.useCallback((tree, id: string, index: number, reset = false) => {
        setNodeParentHighlightedIds((prev) => new Map([[index, reset ? [] : getParentNodes(tree, id)]]));
    }, []);

    const handleValueHover = React.useCallback(
        (on: "table" | "tree", rowIndex: number, hoveredTagProps: HoveredValuedType) => {
            on === "table"
                ? setTreeValueQuery(() => new Map([[rowIndex, hoveredTagProps]]))
                : setTableValueQuery(() => new Map([[rowIndex, hoveredTagProps]]));
        },
        []
    );

    const headerData = [
        {
            key: "source",
            header: t("linkingEvaluationTabView.table.header.source"),
        },
        {
            key: "target",
            header: t("linkingEvaluationTabView.table.header.target"),
        },
        {
            key: "confidence",
            header: t("linkingEvaluationTabView.table.header.score"),
        },
    ];

    const rowData = evaluationResults.current?.links.map((evaluation, i) => ({ ...evaluation, id: `${i}` })) ?? [];

    const handleRowExpansion = React.useCallback(
        (rowId?: number) => {
            setExpandedRows((prevExpandedRows) => {
                if (typeof rowId !== "undefined" && prevExpandedRows.has(rowId)) {
                    prevExpandedRows.delete(rowId);
                    //when universal expansion btn is pressed,
                    //modify input and operator list behavior
                    handleNodeExpand(rowId, showOperators, true);
                    setInputValuesExpansion((prevInputExpansion) => {
                        prevInputExpansion.set(rowId, {
                            expanded: showInputValues,
                            precinct: true,
                        });
                        return new Map(prevInputExpansion);
                    });
                    return new Map([...prevExpandedRows]);
                } else if (typeof rowId !== "undefined") {
                    //provided row id doesn't exist in record
                    return new Map([...prevExpandedRows, [rowId, rowId]]);
                } else {
                    //should either collapse all or expand all.
                    if (prevExpandedRows.size === rowData.length) return new Map();
                    return new Map(rowData.map((_row: any, i: number) => [i, i]));
                }
            });
        },
        [rowData]
    );

    const handleNodeExpand = React.useCallback((nodeIdx: number, isExpanded = true, precinct = false) => {
        setOperatorsExpansion((prev) => {
            prev.set(nodeIdx, {
                expanded: isExpanded,
                precinct: precinct,
            });
            return new Map([...prev]);
        });
    }, []);

    const handleReferenceLinkTypeUpdate = React.useCallback(
        async (
            currentLinkType: ReferenceLinkType,
            linkType: ReferenceLinkType,
            source: string,
            target: string,
            index: number
        ) => {
            if (currentLinkType === linkType) return;
            setLoading(true);

            try {
                const updateResp = await updateReferenceLink(projectId, linkingTaskId, source, target, linkType);
                if (updateResp.axiosResponse.status === 200 && evaluationResults.current) {
                    //update one
                    evaluationResults.current.links[index].decision = linkType;
                }
            } catch (err) {
            } finally {
                setLoading(false);
            }
        },
        []
    );
    const { nrSourceEntities, nrTargetEntities, nrLinks } = evaluationResults.current?.evaluationActivityStats ?? {
        nrSourceEntities: 0,
        nrTargetEntities: 0,
        nrLinks: 0,
    };

    const handleLinkFilterStateChange = React.useCallback(
        (linkState: keyof typeof LinkEvaluationFilters) =>
            setLinkStateFilter((prev) => (prev === linkState ? undefined : linkState)),
        []
    );

    const handleRowSorting = React.useCallback(
        (key: typeof headerData[number]["key"]) => {
            const sortDirection = tableSortDirection.get(key)!;
            setLinkSortBy(() =>
                sortDirectionMapping[sortDirection] === "NONE"
                    ? []
                    : [LinkEvaluationSortByObj[sortDirectionMapping[sortDirection]][key]]
            );
            setTableSortDirection((prev) => {
                prev.set(key, sortDirectionMapping[sortDirection]);
                return new Map([...prev]);
            });
        },
        [tableSortDirection]
    );

    const createUserNotification = React.useCallback(() => {
        if (
            taskEvaluationStatus === "Not executed" ||
            taskEvaluationStatus === "Cancelled" ||
            typeof taskEvaluationStatus === "undefined"
        ) {
            // evalution action did not run
            return (
                <Notification data-test-id="notification-missing-execution">
                    {t("linkingEvaluationTabView.messages.missingExecution")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Failed") {
            // evalution action did run with errors
            return (
                <Notification warning data-test-id="notification-unsuccessful-evaluation">
                    {t("linkingEvaluationTabView.messages.unsuccessfulEvaluation")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Successful" && !searchQuery && !linkStateFilter) {
            // evalution action done, no filters, no results
            return (
                <Notification data-test-id="empty-links-banner">
                    {t("linkingEvaluationTabView.messages.emptyWithoutFilters")}
                </Notification>
            );
        }

        if (taskEvaluationStatus === "Successful" && (!!searchQuery || !!linkStateFilter)) {
            // evalution action done, filters for link state or search query active
            return (
                <Notification data-test-id="notification-empty-with-filters">
                    {t("linkingEvaluationTabView.messages.emptyWithFilters")}
                </Notification>
            );
        }

        // Fallback
        return (
            <Notification warning data-test-id="notification-unknown-problem">
                {t("linkingEvaluationTabView.messages.unknownProblem")}
            </Notification>
        );
    }, [taskEvaluationStatus, searchQuery, linkStateFilter]);

    const handleSwitchChange = React.useCallback((switchType: "inputValue" | "operator") => {
        return (val: boolean) => {
            switchType === "inputValue" ? setShowInputValues(val) : setShowOperators(val);
            handleAlwaysExpandSwitch(switchType);
        };
    }, []);

    const registerForTaskUpdates = React.useCallback((status: any) => {
        setTaskEvaluationStatus(status.concreteStatus);
    }, []);

    const handleInputTableExpansion = React.useCallback((i: number) => {
        return () => {
            setInputValuesExpansion((prevInputExpansion) => {
                prevInputExpansion.set(i, {
                    expanded: !prevInputExpansion.get(i)?.expanded,
                    precinct: false,
                });
                return new Map(prevInputExpansion);
            });
        };
    }, []);

    return (
        <section className="diapp-linking-evaluation">
            <Toolbar noWrap>
                <ToolbarSection canShrink>
                    <Switch
                        id={`input-value-${showInputValues ? "checked" : "unchecked"}`}
                        data-test-id="input-value-switch"
                        checked={showInputValues}
                        onChange={handleSwitchChange("inputValue")}
                    >
                        <OverflowText inline>{t("linkingEvaluationTabView.toolbar.toggleExampleValues")}</OverflowText>
                    </Switch>
                </ToolbarSection>
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <Switch
                        id={`operator-${showInputValues ? "checked" : "unchecked"}`}
                        data-test-id="operator-switch"
                        checked={showOperators}
                        onChange={handleSwitchChange("operator")}
                    >
                        <OverflowText inline>{t("linkingEvaluationTabView.toolbar.toggleOperatorsTree")}</OverflowText>
                    </Switch>
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                {taskEvaluationStatus === "Successful" && (
                    <ToolbarSection canShrink style={{ maxWidth: "25%" }}>
                        <ActivityControlWidget
                            border
                            small
                            canShrink
                            label={
                                <strong>
                                    {t("linkingEvaluationTabView.toolbar.statsHeadSources")}
                                    {" / "}
                                    {t("linkingEvaluationTabView.toolbar.statsHeadTargets")}
                                    {" / "}
                                    {t("linkingEvaluationTabView.toolbar.statsHeadLinkCount")}
                                </strong>
                            }
                            statusMessage={`${nrSourceEntities.toLocaleString(
                                commonSel.locale
                            )} / ${nrTargetEntities.toLocaleString(commonSel.locale)} / ${nrLinks.toLocaleString(
                                commonSel.locale
                            )}`}
                            activityActions={[
                                {
                                    icon: "item-info",
                                    action: () => {}, // TODO
                                    tooltip: t("linkingEvaluationTabView.toolbar.statsShowInfo"),
                                },
                            ]}
                        />
                    </ToolbarSection>
                )}
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <TaskActivityWidget
                        projectId={projectId}
                        taskId={linkingTaskId}
                        label="Evaluate Linking"
                        activityName="EvaluateLinking"
                        registerToReceiveUpdates={registerForTaskUpdates}
                        layoutConfig={{
                            small: true,
                            border: true,
                            hasSpacing: true,
                            canShrink: true,
                        }}
                    />
                </ToolbarSection>
            </Toolbar>
            <Divider addSpacing="medium" />
            <SearchField
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onClearanceHandler={() => setSearchQuery("")}
            />
            <Spacing size="small" />
            {linkStateFilter && (
                <TagList label="Link State">
                    <Tag onRemove={() => handleLinkFilterStateChange(linkStateFilter)}>
                        {LinkEvaluationFilters[linkStateFilter].label}
                    </Tag>
                </TagList>
            )}
            <Spacing size="small" />
            <TableContainer rows={rowData} headers={headerData}>
                {({ rows, headers, getHeaderProps, getTableProps, getRowProps }) => (
                    <Table
                        {...getTableProps()}
                        size="medium"
                        columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}
                        useZebraStyles
                    >
                        <TableHead>
                            <TableRow>
                                <TableExpandHeader
                                    enableToggle
                                    isExpanded={expandedRows.size === rowData.length}
                                    onExpand={() => handleRowExpansion()}
                                    togglerText={
                                        expandedRows.size === rowData.length
                                            ? t("linkingEvaluationTabView.table.header.collapseRows")
                                            : t("linkingEvaluationTabView.table.header.expandRows")
                                    }
                                />
                                {headers.map((header) => (
                                    <TableHeader
                                        key={header.key}
                                        data-test-id={header.key}
                                        className={tableSortDirection.get(header.key)}
                                        {...getHeaderProps({ header, isSortable: true })}
                                        isSortHeader={true}
                                        onClick={() => {
                                            handleRowSorting(header.key);
                                        }}
                                        sortDirection={tableSortDirection.get(header.key)}
                                    >
                                        {header.header}
                                    </TableHeader>
                                ))}
                                <TableHeader>
                                    {t("linkingEvaluationTabView.table.header.linkState")}
                                    <Spacing vertical size="tiny" />
                                    <ContextMenu togglerElement="operation-filter">
                                        <MenuItem
                                            text={t("ReferenceLinks.confirmed")}
                                            active={linkStateFilter === LinkEvaluationFilters.positiveLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.positiveLinks.key);
                                            }}
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.uncertainOnly")}
                                            active={linkStateFilter === LinkEvaluationFilters.undecidedLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.undecidedLinks.key);
                                            }}
                                        />
                                        <MenuItem
                                            text={t("ReferenceLinks.declined")}
                                            active={linkStateFilter === LinkEvaluationFilters.negativeLinks.key}
                                            onClick={() => {
                                                handleLinkFilterStateChange(LinkEvaluationFilters.negativeLinks.key);
                                            }}
                                        />
                                    </ContextMenu>
                                </TableHeader>
                            </TableRow>
                        </TableHead>
                        {(evaluationResults.current && evaluationResults.current.links.length && !loading && (
                            <TableBody>
                                {rows.map((row, i) => {
                                    const currentInputValue = inputValues[i];
                                    const currentLink = evaluationResults.current?.links[i]!;
                                    const inputTableIsExpanded = inputValuesExpansion.get(i)?.expanded;
                                    const tableValueToHighlight = tableValueQuery.get(i);
                                    const highlightSourceTableValue = (currentPath: string, isSourceEntity: boolean) =>
                                        tableValueToHighlight &&
                                        tableValueToHighlight.isSourceEntity === isSourceEntity &&
                                        currentPath === tableValueToHighlight.path
                                            ? new Set([tableValueToHighlight.value])
                                            : undefined;
                                    return (
                                        <React.Fragment key={i}>
                                            {currentLink && (
                                                <TableExpandRow
                                                    {...getRowProps({ row })}
                                                    key={row.id}
                                                    isExpanded={expandedRows.has(i)}
                                                    onExpand={() => handleRowExpansion(i)}
                                                    togglerText={
                                                        expandedRows.has(i)
                                                            ? t("linkingEvaluationTabView.table.collapseRow")
                                                            : t("linkingEvaluationTabView.table.expandRow")
                                                    }
                                                    className="diapp-linking-evaluation__row-item"
                                                >
                                                    {row.cells.map((cell) => {
                                                        const [, rowKey] = cell.id.split(":");
                                                        return rowKey === "confidence" ? (
                                                            <TableCell key="confidence">
                                                                <ConfidenceValue value={currentLink.confidence} />
                                                            </TableCell>
                                                        ) : (
                                                            <TableCell key={rowKey}>
                                                                <Highlighter
                                                                    label={cell.value}
                                                                    searchValue={searchQuery}
                                                                />
                                                            </TableCell>
                                                        );
                                                    })}
                                                    <TableCell key="linkstate">
                                                        <div style={{ whiteSpace: "nowrap" }}>
                                                            {linkStateButtons.map(
                                                                ({ linkType, icon, ...otherProps }, btnIndex) => (
                                                                    <React.Fragment key={icon}>
                                                                        <IconButton
                                                                            name={icon}
                                                                            onClick={() =>
                                                                                handleReferenceLinkTypeUpdate(
                                                                                    currentLink.decision,
                                                                                    linkType,
                                                                                    currentLink.source,
                                                                                    currentLink.target,
                                                                                    i
                                                                                )
                                                                            }
                                                                            {...otherProps}
                                                                            outlined={currentLink.decision !== linkType}
                                                                            minimal={false}
                                                                        />
                                                                        {btnIndex !== linkStateButtons.length - 1 && (
                                                                            <Spacing vertical size="tiny" />
                                                                        )}
                                                                    </React.Fragment>
                                                                )
                                                            )}
                                                        </div>
                                                    </TableCell>
                                                </TableExpandRow>
                                            )}
                                            {!!currentInputValue && expandedRows.has(i) && (
                                                <TableExpandedRow
                                                    colSpan={headers.length + 2}
                                                    className="linking-table__expanded-row-container"
                                                >
                                                    <Table
                                                        size="small"
                                                        columnWidths={["30px", "40%", "40%", "7rem", "9rem"]}
                                                        hasDivider={false}
                                                        colorless
                                                    >
                                                        <TableBody>
                                                            <TableRow>
                                                                <TableCell
                                                                    style={{ paddingLeft: "0", paddingRight: "0" }}
                                                                >
                                                                    <IconButton
                                                                        data-test-id="input-table-expand-btn"
                                                                        id={`input-table-${
                                                                            inputValuesExpansion.get(i)?.expanded
                                                                                ? "expanded"
                                                                                : "collapsed"
                                                                        }`}
                                                                        onClick={handleInputTableExpansion(i)}
                                                                        name={
                                                                            !inputValuesExpansion.get(i)?.expanded
                                                                                ? "toggler-caretright"
                                                                                : "toggler-caretdown"
                                                                        }
                                                                    />
                                                                </TableCell>
                                                                <TableCell style={{ verticalAlign: "middle" }}>
                                                                    {!inputTableIsExpanded && (
                                                                        <OverflowText>
                                                                            {t(
                                                                                "linkingEvaluationTabView.table.infoCollapsedInputValue"
                                                                            )}
                                                                        </OverflowText>
                                                                    )}
                                                                    {!!inputTableIsExpanded && (
                                                                        <ComparisonDataContainer>
                                                                            {Object.entries(
                                                                                currentInputValue.source
                                                                            ).map(([key, values]) => (
                                                                                <ComparisonDataCell
                                                                                    key={key}
                                                                                    fullWidth
                                                                                    className={
                                                                                        !inputTableIsExpanded
                                                                                            ? "shrink"
                                                                                            : ""
                                                                                    }
                                                                                >
                                                                                    <PropertyBox
                                                                                        propertyName={key}
                                                                                        exampleValues={
                                                                                            <ActiveLearningValueExamples
                                                                                                interactive
                                                                                                valuesToHighlight={highlightSourceTableValue(
                                                                                                    key,
                                                                                                    true
                                                                                                )}
                                                                                                onHover={(val) =>
                                                                                                    handleValueHover(
                                                                                                        "table",
                                                                                                        i,
                                                                                                        {
                                                                                                            path: key,
                                                                                                            isSourceEntity:
                                                                                                                true,
                                                                                                            value: val,
                                                                                                        }
                                                                                                    )
                                                                                                }
                                                                                                exampleValues={
                                                                                                    values ?? []
                                                                                                }
                                                                                            />
                                                                                        }
                                                                                    />
                                                                                </ComparisonDataCell>
                                                                            ))}
                                                                        </ComparisonDataContainer>
                                                                    )}
                                                                </TableCell>
                                                                <TableCell>
                                                                    {!!inputTableIsExpanded && (
                                                                        <ComparisonDataContainer>
                                                                            {Object.entries(
                                                                                currentInputValue.target
                                                                            ).map(([key, values]) => (
                                                                                <ComparisonDataCell
                                                                                    key={key}
                                                                                    fullWidth
                                                                                    className={
                                                                                        !inputTableIsExpanded
                                                                                            ? "shrink"
                                                                                            : ""
                                                                                    }
                                                                                >
                                                                                    <PropertyBox
                                                                                        propertyName={key}
                                                                                        exampleValues={
                                                                                            <ActiveLearningValueExamples
                                                                                                interactive
                                                                                                valuesToHighlight={highlightSourceTableValue(
                                                                                                    key,
                                                                                                    false
                                                                                                )}
                                                                                                onHover={(val) =>
                                                                                                    handleValueHover(
                                                                                                        "table",
                                                                                                        i,
                                                                                                        {
                                                                                                            path: key,
                                                                                                            isSourceEntity:
                                                                                                                false,
                                                                                                            value: val,
                                                                                                        }
                                                                                                    )
                                                                                                }
                                                                                                exampleValues={
                                                                                                    values ?? []
                                                                                                }
                                                                                            />
                                                                                        }
                                                                                    />
                                                                                </ComparisonDataCell>
                                                                            ))}
                                                                        </ComparisonDataContainer>
                                                                    )}
                                                                </TableCell>
                                                            </TableRow>
                                                        </TableBody>
                                                    </Table>
                                                    <TableTree
                                                        treeIsExpanded={!!operatorsExpansion.get(i)?.expanded}
                                                        nodes={[nodes[i]]}
                                                        handleTableExpansion={(shouldExpand) =>
                                                            handleNodeExpand(i, shouldExpand)
                                                        }
                                                    />
                                                </TableExpandedRow>
                                            )}
                                        </React.Fragment>
                                    );
                                })}
                            </TableBody>
                        )) ||
                            null}
                    </Table>
                )}
            </TableContainer>
            <Spacing size="small" />
            {(loading && <Spinner size="medium" />) ||
                (!evaluationResults.current?.links?.length && createUserNotification())}
            {!!evaluationResults.current?.links.length && paginationElement}
        </section>
    );
};

export default LinkingEvaluationTabView;
