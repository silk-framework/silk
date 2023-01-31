import {
    ActivityControlWidget,
    ConfidenceValue,
    ContextMenu,
    Divider,
    Grid,
    GridColumn,
    GridRow,
    Highlighter,
    IActivityStatus,
    IconButton,
    MenuItem,
    Notification,
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
    Tree,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../../views/shared/TaskActivityWidget/TaskActivityWidget";
import Pagination from "../../../../../views/shared/Pagination";
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
    LinkingEvaluationResult,
    LinkStats,
    NodePath,
    ReferenceLinkType,
} from "./typings";
import utils from "../LinkingRuleEvaluation.utils";
import { ComparisonDataCell, ComparisonDataContainer } from "../../activeLearning/components/ComparisionData";
import { ActiveLearningValueExamples } from "../../activeLearning/shared/ActiveLearningValueExamples";
import { PropertyBox } from "../../activeLearning/components/PropertyBox";
import { IAggregationOperator, IComparisonOperator, ILinkingRule } from "../../linking.types";
import { TreeNodeInfo } from "@blueprintjs/core";
import { EvaluationResultType } from "../LinkingRuleEvaluation";
import { tagColor } from "../../../../../views/shared/RuleEditor/view/sidebar/RuleOperator";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { debounce } from "lodash";
import { workspaceSel } from "@ducks/workspace";
import { useSelector } from "react-redux";
import { SortRowData } from "carbon-components-react";

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

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const commonSel = useSelector(workspaceSel.commonSelector);
    const [evaluationResults, setEvaluationResults] = React.useState<
        { links: Array<LinkingEvaluationResult>; linkRule: ILinkingRule; stats: LinkStats } | undefined
    >();
    const [pagination, setPagination] = React.useState<{ current: number; total: number; limit: number }>({
        current: 1,
        total: 25,
        limit: 10,
    });
    const [loading, setLoading] = React.useState<boolean>(false);
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<string, string>>(new Map());
    const [nodes, setNodes] = React.useState<TreeNodeInfo[]>([]);
    const [linksToValueMap, setLinksToValueMap] = React.useState<Array<Map<string, EvaluationResultType[number]>>>([]);
    const [inputValuesExpansion, setInputValuesExpansion] = React.useState<
        Map<number, { expanded: boolean; precinct: boolean }>
    >(new Map());
    const [operatorsExpansion, setOperatorsExpansion] = React.useState<
        Map<number, { expanded: boolean; precinct: boolean }>
    >(new Map());
    const [tableValueQuery, setTableValueQuery] = React.useState<Map<number, HoveredValuedType>>(new Map());
    const [treeValueQuery, setTreeValueQuery] = React.useState<Map<number, HoveredValuedType>>(new Map());
    const [taskEvaluationStatus, setTaskEvaluationStatus] = React.useState<IActivityStatus["statusName"] | undefined>();
    const [operatorPlugins, setOperatorPlugins] = React.useState<Array<IPluginDetails>>([]);
    const [nodeParentHighlightedIds, setNodeParentHighlightedIds] = React.useState<Map<number, string[]>>(new Map());
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    const [linkStateFilters, setLinkStateFilters] = React.useState<Map<LinkEvaluationFilters, boolean>>(new Map());
    const [linkSortBy, setLinkSortBy] = React.useState<Array<LinkEvaluationSortBy>>([]);
    const [updateCounter, setUpdateCounter] = React.useState<number>(0);

    //fetch operator plugins
    React.useEffect(() => {
        (async () => {
            setOperatorPlugins(Object.values((await requestRuleOperatorPluginDetails(false)).data));
        })();
    }, []);

    const debouncedInit = React.useCallback(
        debounce(async (pagination, searchQuery, taskEvaluationStatus, filters, linkSortBy) => {
            try {
                setLoading(true);
                if (taskEvaluationStatus === "Finished") {
                    const results = (
                        await getEvaluatedLinks(projectId, linkingTaskId, pagination, searchQuery, filters, linkSortBy)
                    )?.data;
                    setEvaluationResults(results);
                    setLinksToValueMap(results?.links.map((link) => utils.linkToValueMap(link as any)) ?? []);
                    setInputValuesExpansion(
                        () =>
                            new Map(
                                results?.links.map((_, idx) => [idx, { expanded: showInputValues, precinct: true }])
                            )
                    );
                    setOperatorsExpansion(
                        () =>
                            new Map(results?.links.map((_, idx) => [idx, { expanded: showOperators, precinct: true }]))
                    );
                }
            } catch (err) {
            } finally {
                setLoading(false);
            }
        }, 500),
        []
    );

    //initial loads of links
    React.useEffect(() => {
        let shouldCancel = false;

        if (!shouldCancel) {
            debouncedInit(pagination, searchQuery, taskEvaluationStatus, [...linkStateFilters.keys()], linkSortBy);
        }
        return () => {
            shouldCancel = true;
        };
    }, [pagination, taskEvaluationStatus, searchQuery, linkStateFilters.size, linkSortBy, updateCounter]);

    const handleAlwaysExpandSwitch = React.useCallback(
        (inputSwitch: "operator" | "inputValue") => {
            if (evaluationResults?.links.length) {
                //if expansion buttons have been triggered by user, maintain user's update while changing the default of the rest.
                const newUpdate = (prev) =>
                    new Map(
                        evaluationResults?.links.map((_: any, idx: number) => {
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

    React.useEffect(() => {
        if (!evaluationResults || !linksToValueMap.length) return;
        const operatorNode = evaluationResults?.linkRule.operator as any;
        setNodes(() =>
            new Array(evaluationResults?.links.length).fill(1).map((_, idx) => {
                const treeInfo: TreeNodeInfo = {
                    id: operatorNode.id,
                    isExpanded: operatorsExpansion.get(idx)?.expanded ?? false,
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

                    ["sourceInput", "targetInput"].forEach((inputPath) => {
                        const isSourceEntity = inputPath === "sourceInput";
                        //is comparison operator
                        let inputNode: TreeNodeInfo = {
                            id: node[inputPath].id,
                            isExpanded: true,
                            hasCaret: false,
                            label: (
                                <p>
                                    <Tag
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
                                    <Spacing vertical size="tiny" />
                                    {getLinkValues(node[inputPath].id, idx, treeInfo, {
                                        path: node[inputPath].path ?? "",
                                        isSourceEntity,
                                    })}
                                </p>
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
                    parentTree && treeInfo.childNodes!.push(parentTree);
                };

                if (operatorNode.inputs?.length) {
                    //Aggregation operator
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
                    });
                } else {
                    getSubTree(operatorNode);
                }
                return treeInfo;
            })
        );
    }, [
        evaluationResults,
        linksToValueMap,
        pagination,
        treeValueQuery,
        operatorPlugins,
        nodeParentHighlightedIds,
        operatorsExpansion,
    ]);

    React.useEffect(() => {
        if (evaluationResults && evaluationResults.linkRule && evaluationResults.links && linksToValueMap.length) {
            const ruleOperator = evaluationResults.linkRule.operator;
            if (ruleOperator) {
                const inputPaths = (ruleOperator as IComparisonOperator)?.sourceInput
                    ? getLinkRuleInputPaths(ruleOperator)
                    : ((ruleOperator as IAggregationOperator)?.inputs ?? []).reduce(
                          (acc, input) => {
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
                              return acc;
                          },
                          { source: {}, target: {} } as EvaluationLinkInputValue<string>
                      );

                setInputValues(() =>
                    linksToValueMap.map((linkToValueMap) =>
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
    }, [evaluationResults, linksToValueMap, pagination]);

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
                    <p>
                        <Tag backgroundColor={tagColor(tagInputTag) as string}>
                            {getOperatorLabel(input, operatorPlugins)}
                        </Tag>
                        <Spacing vertical size="tiny" />
                        {getLinkValues(input.id, index, parentTree, {
                            path: input.path ?? "",
                            isSourceEntity,
                        })}
                    </p>
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
                    <p>
                        <Tag backgroundColor={tagColor(operatorInputMapping[input.type]) as string}>
                            {getOperatorLabel(input, operatorPlugins)}
                        </Tag>
                        <Spacing vertical size="tiny" />
                        {getLinkValues(input.id, index, parentTree, {
                            path: input.path ?? "",
                            isSourceEntity,
                        })}
                    </p>
                ),
            };
            tree.childNodes = [...(tree?.childNodes ?? []), newChildTree];

            acc = buildInputTree(i, newChildTree, index, tagInputTag, parentTree, isSourceEntity);
            return acc;
        }, {} as TreeNodeInfo);
    };

    const getOperatorConfidence = React.useCallback(
        (id: string, index: number) => {
            const linkToValueMap = linksToValueMap[index];
            if (!linksToValueMap.length || !linkToValueMap) return <></>;
            return linkToValueMap.get(id)?.value.map((val, i) => (
                <ConfidenceValue
                    key={i}
                    value={val.includes("Score") ? Number(val.replace("Score: ", "")) : Number(val)}
                    spaceUsage="minimal"
                    tagProps={{
                        backgroundColor: nodeParentHighlightedIds.get(index)?.includes(id) ? "#0097a7" : undefined,
                    }}
                />
            ));
        },
        [linksToValueMap, nodeParentHighlightedIds, pagination]
    );

    const getLinkValues = React.useCallback(
        (id: string, index: number, tree: TreeNodeInfo, nodeData: Omit<HoveredValuedType, "value">) => {
            const linkToValueMap = linksToValueMap[index];
            if (linksToValueMap.length && linkToValueMap && nodeData) {
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
                return (
                    <TagList>
                        {linkToValueMap.get(id)?.value.map((val, i) => (
                            <React.Fragment key={val + i}>
                                <Tag
                                    round
                                    emphasis="stronger"
                                    interactive
                                    backgroundColor={
                                        isHighlightMatch(val)
                                            ? "#746a85"
                                            : nodeParentHighlightedIds.get(index)?.includes(id)
                                            ? "#0097a7"
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
                                <Spacing vertical size="tiny" />
                            </React.Fragment>
                        ))}
                    </TagList>
                );
            }
        },
        [linksToValueMap, treeValueQuery, nodeParentHighlightedIds]
    );

    const handleParentNodeHighlights = React.useCallback((tree, id: string, index: number, reset = false) => {
        setNodeParentHighlightedIds((prev) => new Map([...prev, [index, reset ? [] : getParentNodes(tree, id)]]));
    }, []);

    const handleValueHover = React.useCallback(
        (on: "table" | "tree", rowIndex: number, hoveredTagProps: HoveredValuedType) => {
            on === "table"
                ? setTreeValueQuery((prev) => new Map([...prev, [rowIndex, hoveredTagProps]]))
                : setTableValueQuery((prev) => new Map([...prev, [rowIndex, hoveredTagProps]]));
        },
        []
    );

    const handlePagination = React.useCallback((page: number, limit: number) => {
        setPagination({ current: page, total: 25, limit });
    }, []);

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

    const rowData = evaluationResults?.links.map((evaluation, i) => ({ ...evaluation, id: `${i}` })) ?? [];

    const handleRowExpansion = React.useCallback(
        (rowId?: string) => {
            setExpandedRows((prevExpandedRows) => {
                if (rowId && prevExpandedRows.has(rowId)) {
                    prevExpandedRows.delete(rowId);
                    return new Map([...prevExpandedRows]);
                } else if (rowId) {
                    return new Map([...prevExpandedRows, [rowId, rowId]]);
                } else {
                    if (prevExpandedRows.size === rowData.length) return new Map();
                    return new Map(rowData.map((row) => [row.id, row.id]));
                }
            });
        },
        [rowData]
    );

    const handleNodeExpand = React.useCallback((nodeIdx: number, isExpanded = true) => {
        setNodes((prevTreeNodes) =>
            prevTreeNodes.map((prevTreeNode, i) =>
                i === nodeIdx
                    ? {
                          ...prevTreeNode,
                          isExpanded,
                      }
                    : prevTreeNode
            )
        );
        setOperatorsExpansion((prev) => {
            prev.set(nodeIdx, {
                expanded: isExpanded,
                precinct: false,
            });
            return new Map([...prev]);
        });
    }, []);

    const handleReferenceLinkTypeUpdate = React.useCallback(
        async (currentLinkType: ReferenceLinkType, linkType: ReferenceLinkType, source: string, target: string) => {
            if (currentLinkType === linkType) return;
            try {
                const updateResp = await updateReferenceLink(projectId, linkingTaskId, source, target, linkType);
                if (updateResp.axiosResponse.status === 200) {
                    setUpdateCounter((u) => ++u);
                }
            } catch (err) {}
        },
        []
    );
    const { nrSourceEntities, nrTargetEntities, nrLinks } = evaluationResults?.stats ?? {
        nrSourceEntities: 0,
        nrTargetEntities: 0,
        nrLinks: 0,
    };

    const handleLinkFilterStateChange = React.useCallback(
        (linkState: LinkEvaluationFilters, checked: boolean) =>
            setLinkStateFilters((prev) => {
                if (checked && !prev.has(linkState)) {
                    return new Map([...prev, [linkState, true]]);
                } else if (!checked && prev.has(linkState)) {
                    prev.delete(linkState);
                    return new Map([...prev]);
                }
                return prev;
            }),
        []
    );

    const handleRowSorting = React.useCallback((cellA, cellB, { sortDirection, sortStates, key }: SortRowData) => {
        setLinkSortBy([LinkEvaluationSortByObj[sortDirection][key]]);
        return 0;
    }, []);

    return (
        <section className="linking-evaluation">
            <Toolbar noWrap>
                <ToolbarSection>
                    <Switch
                        checked={showInputValues}
                        onChange={(val) => {
                            setShowInputValues(val);
                            handleAlwaysExpandSwitch("inputValue");
                        }}
                        label="always expand input values"
                    />
                </ToolbarSection>
                <ToolbarSection>
                    <Spacing vertical size="small" />
                    <Switch
                        checked={showOperators}
                        onChange={(val) => {
                            setShowOperators(val);
                            handleAlwaysExpandSwitch("operator");
                        }}
                        label="always expand operators"
                    />
                </ToolbarSection>
                <ToolbarSection canGrow>
                    <Spacing vertical />
                </ToolbarSection>
                <ToolbarSection canShrink>
                    <ActivityControlWidget
                        border
                        small
                        canShrink
                        label={<strong>Sources / Targets / Links</strong>}
                        statusMessage={`${nrSourceEntities.toLocaleString(
                            commonSel.locale
                        )} / ${nrTargetEntities.toLocaleString(commonSel.locale)} / ${nrLinks.toLocaleString(
                            commonSel.locale
                        )}`}
                        activityActions={[
                            {
                                icon: "item-info",
                                action: () => {}, // TODO
                                tooltip: "evaluation statistics",
                            },
                        ]}
                    />
                </ToolbarSection>
                <ToolbarSection canShrink>
                    <Spacing vertical size="small" />
                    <TaskActivityWidget
                        projectId={projectId}
                        taskId={linkingTaskId}
                        label="Evaluate Linking"
                        activityName="EvaluateLinking"
                        registerToReceiveUpdates={(status) => setTaskEvaluationStatus(status.statusName)}
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
            <SearchField value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
            <Spacing size="small" />
            {evaluationResults && evaluationResults.links.length && !loading ? (
                <TableContainer rows={rowData} headers={headerData} sortRow={handleRowSorting}>
                    {({ rows, headers, getHeaderProps, getTableProps, getRowProps }) => (
                        <Table {...getTableProps()} useZebraStyles>
                            <TableHead>
                                <TableRow>
                                    <TableExpandHeader
                                        enableToggle
                                        isExpanded={expandedRows.size === rowData.length}
                                        onExpand={() => handleRowExpansion()}
                                    />
                                    {headers.map((header) => (
                                        <TableHeader key={header.key} {...getHeaderProps({ header, isSortable: true })}>
                                            {header.header}
                                        </TableHeader>
                                    ))}
                                    <TableHeader>
                                        {t("linkingEvaluationTabView.table.header.linkState")}
                                        <Spacing vertical size="tiny" />
                                        <ContextMenu togglerElement="operation-filter">
                                            <MenuItem
                                                text={"Confirmed"}
                                                icon={
                                                    linkStateFilters.has(LinkEvaluationFilters.positive)
                                                        ? "state-checked"
                                                        : "state-unchecked"
                                                }
                                                onClick={() => {
                                                    handleLinkFilterStateChange(
                                                        LinkEvaluationFilters.positive,
                                                        !linkStateFilters.has(LinkEvaluationFilters.positive)
                                                    );
                                                }}
                                            />
                                            <MenuItem
                                                text={"Declined"}
                                                icon={
                                                    linkStateFilters.has(LinkEvaluationFilters.negative)
                                                        ? "state-checked"
                                                        : "state-unchecked"
                                                }
                                                onClick={() => {
                                                    handleLinkFilterStateChange(
                                                        LinkEvaluationFilters.negative,
                                                        !linkStateFilters.has(LinkEvaluationFilters.negative)
                                                    );
                                                }}
                                            />
                                        </ContextMenu>
                                    </TableHeader>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {rows.map((row, i) => {
                                    const currentInputValue = inputValues[i];
                                    const currentLink = evaluationResults?.links[i]!;
                                    const inputTableIsExpanded = inputValuesExpansion.get(i)?.expanded;
                                    const tableValueToHighlight = tableValueQuery.get(i);
                                    const highlightSourceTableValue = (currentPath: string, isSourceEntity: boolean) =>
                                        tableValueToHighlight &&
                                        tableValueToHighlight.isSourceEntity === isSourceEntity &&
                                        currentPath === tableValueToHighlight.path
                                            ? new Set([tableValueToHighlight.value])
                                            : undefined;
                                    return (
                                        <>
                                            {currentLink && (
                                                <TableExpandRow
                                                    {...getRowProps({ row })}
                                                    key={row.id}
                                                    isExpanded={expandedRows.has(row.id)}
                                                    onExpand={() => handleRowExpansion(row.id)}
                                                    ariaLabel="Links expansion"
                                                    className="linking-evaluation__row-item"
                                                >
                                                    {row.cells.map((cell) => {
                                                        const [, rowKey] = cell.id.split(":");
                                                        return rowKey === "confidence" ? (
                                                            <TableCell>
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
                                                    <TableCell>
                                                        {linkStateButtons.map(
                                                            ({ linkType, icon, ...otherProps }, i) => (
                                                                <React.Fragment key={icon}>
                                                                    <IconButton
                                                                        name={icon}
                                                                        //active={currentLink.decision === name}
                                                                        onClick={() =>
                                                                            handleReferenceLinkTypeUpdate(
                                                                                currentLink.decision,
                                                                                linkType,
                                                                                currentLink.source,
                                                                                currentLink.target
                                                                            )
                                                                        }
                                                                        {...otherProps}
                                                                        outlined={currentLink.decision !== linkType}
                                                                        minimal={false}
                                                                    />
                                                                    {i !== linkStateButtons.length - 1 && (
                                                                        <Spacing vertical size="tiny" />
                                                                    )}
                                                                </React.Fragment>
                                                            )
                                                        )}
                                                    </TableCell>
                                                </TableExpandRow>
                                            )}
                                            {!!currentInputValue && (
                                                <TableExpandedRow
                                                    colSpan={headers.length + 2}
                                                    className="linking-table__expanded-row-container"
                                                >
                                                    <Grid>
                                                        <GridRow>
                                                            <span>
                                                                <IconButton
                                                                    small
                                                                    onClick={() => {
                                                                        setInputValuesExpansion(
                                                                            (prevInputExpansion) => {
                                                                                prevInputExpansion.set(i, {
                                                                                    expanded:
                                                                                        !prevInputExpansion.get(i)
                                                                                            ?.expanded,
                                                                                    precinct: false,
                                                                                });
                                                                                return new Map(prevInputExpansion);
                                                                            }
                                                                        );
                                                                    }}
                                                                    name={
                                                                        !inputValuesExpansion.get(i)?.expanded
                                                                            ? "toggler-moveright"
                                                                            : "toggler-showmore"
                                                                    }
                                                                />
                                                                <Spacing vertical size="tiny" />
                                                                {!inputTableIsExpanded ? (
                                                                    <span className="">Input values collapsed</span>
                                                                ) : null}
                                                            </span>
                                                            <GridColumn medium>
                                                                <ComparisonDataContainer>
                                                                    {Object.entries(currentInputValue.source).map(
                                                                        ([key, values]) => (
                                                                            <ComparisonDataCell
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
                                                                                            exampleValues={values ?? []}
                                                                                        />
                                                                                    }
                                                                                />
                                                                            </ComparisonDataCell>
                                                                        )
                                                                    )}
                                                                </ComparisonDataContainer>
                                                            </GridColumn>
                                                            <GridColumn medium>
                                                                <ComparisonDataContainer>
                                                                    {Object.entries(currentInputValue.target).map(
                                                                        ([key, values]) => (
                                                                            <ComparisonDataCell
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
                                                                                            exampleValues={values ?? []}
                                                                                        />
                                                                                    }
                                                                                />
                                                                            </ComparisonDataCell>
                                                                        )
                                                                    )}
                                                                </ComparisonDataContainer>
                                                            </GridColumn>
                                                        </GridRow>
                                                        <Spacing size="tiny" />
                                                        <GridRow>
                                                            <Tree
                                                                contents={[nodes[i] ?? []]}
                                                                onNodeCollapse={(
                                                                    _node: TreeNodeInfo,
                                                                    nodePath: NodePath
                                                                ) => handleNodeExpand(i, false)}
                                                                onNodeExpand={(
                                                                    _node: TreeNodeInfo,
                                                                    nodePath: NodePath
                                                                ) => handleNodeExpand(i)}
                                                            />
                                                        </GridRow>
                                                    </Grid>
                                                </TableExpandedRow>
                                            )}
                                        </>
                                    );
                                })}
                            </TableBody>
                        </Table>
                    )}
                </TableContainer>
            ) : (
                (loading && <Spinner size="medium" />) || (
                    <Notification data-test-id="empty-links-banner">{t("linkingEvaluationTabView.empty")}</Notification>
                )
            )}
            <Spacing size="small" />
            {!!evaluationResults?.links.length && (
                <Pagination pagination={pagination} pageSizes={[10, 25, 50, 100]} onChangeSelect={handlePagination} />
            )}
        </section>
    );
};

export default LinkingEvaluationTabView;
