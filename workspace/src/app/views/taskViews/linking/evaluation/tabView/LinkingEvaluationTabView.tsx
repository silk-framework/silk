import {
    Grid,
    GridRow,
    SearchField,
    Spacing,
    Table,
    TableBody,
    TableHead,
    TableHeader,
    TableRow,
    TableCell,
    GridColumn,
    OverviewItem,
    Card,
    IconButton,
    OverviewItemLine,
    Icon,
    OverviewItemDescription,
    OverviewItemActions,
    Switch,
    ContextMenu,
    MenuItem,
    DataTable,
    TableExpandRow,
    TableExpandedRow,
    TableContainer,
    Tag,
    Tree,
    TagList,
    IActivityStatus,
    ConfidenceValue,
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../../views/shared/TaskActivityWidget/TaskActivityWidget";
import Pagination from "../../../../../views/shared/Pagination";
import {
    getLinkingEvaluations,
    getLinkRuleInputPaths,
    getOperatorLabel,
    getParentNodes,
} from "./LinkingEvaluationViewUtils";
import { EvaluationLinkInputValue, LinkingEvaluationResult, NodePath } from "./typings";
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

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const operatorInputMapping = {
    transformInput: "Transform",
    pathInput: "Input",
};

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const [evaluationResults, setEvaluationResults] = React.useState<
        { links: Array<LinkingEvaluationResult>; linkRule: ILinkingRule } | undefined
    >();
    const [pagination, setPagination] = React.useState<{ current: number; total: number; limit: number }>({
        current: 1,
        total: 25,
        limit: 10,
    });
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<string, string>>(new Map());
    const [nodes, setNodes] = React.useState<TreeNodeInfo[]>([]);
    const [linksToValueMap, setLinksToValueMap] = React.useState<Array<Map<string, EvaluationResultType[number]>>>([]);
    const [inputValuesExpansion, setInputValuesExpansion] = React.useState<Map<number, boolean>>(new Map());
    const [tableValueQuery, setTableValueQuery] = React.useState<Map<number, string>>(new Map());
    const [treeValueQuery, setTreeValueQuery] = React.useState<Map<number, string>>(new Map());
    const [taskEvaluationStatus, setTaskEvaluationStatus] = React.useState<IActivityStatus["statusName"] | undefined>();
    const [operatorPlugins, setOperatorPlugins] = React.useState<Array<IPluginDetails>>([]);
    const [nodeParentHighlightedIds, setNodeParentHighlightedIds] = React.useState<Map<number, string[]>>(new Map());
    const [searchQuery, setSearchQuery] = React.useState<string>("");
    //fetch operator plugins
    React.useEffect(() => {
        (async () => {
            setOperatorPlugins(Object.values((await requestRuleOperatorPluginDetails(false)).data));
        })();
    }, []);

    //initial loads of links
    React.useEffect(() => {
        debounce(async () => {
            if (taskEvaluationStatus === "Finished") {
                const results = (await getLinkingEvaluations(projectId, linkingTaskId, pagination, searchQuery))?.data;
                setEvaluationResults(results);
                setLinksToValueMap(results?.links.map((link) => utils.linkToValueMap(link as any)) ?? []);
                setInputValuesExpansion(() => new Map(results?.links.map((_, idx) => [idx, false])));
            }
        }, 500)();
    }, [pagination, taskEvaluationStatus, searchQuery]);

    React.useEffect(() => {
        if (!evaluationResults || !linksToValueMap.length) return;
        const operatorNode = evaluationResults?.linkRule.operator as any;
        setNodes(() =>
            new Array(evaluationResults?.links.length).fill(1).map((_, idx) => {
                const treeInfo: TreeNodeInfo = {
                    id: operatorNode.id,
                    isExpanded: true,
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
                                    {getLinkValues(node[inputPath].id, idx, treeInfo)}
                                </p>
                            ),
                            childNodes: [],
                        };

                        if (node[inputPath].inputs?.length) {
                            node[inputPath].inputs.forEach((i) => {
                                inputNode = buildInputTree(i, inputNode, idx, inputPathCategory[inputPath], treeInfo);
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
    }, [evaluationResults, linksToValueMap, pagination, treeValueQuery, operatorPlugins, nodeParentHighlightedIds]);

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
        parentTree: TreeNodeInfo
    ): TreeNodeInfo => {
        if (!input.inputs?.length) {
            return {
                ...tree,
                childNodes: [
                    ...(tree.childNodes ?? []),
                    {
                        id: input.id,
                        hasCaret: false,
                        isExpanded: true,
                        label: (
                            <p>
                                <Tag backgroundColor={tagColor(tagInputTag) as string}>
                                    {getOperatorLabel(input, operatorPlugins)}
                                </Tag>
                                <Spacing vertical size="tiny" />
                                {getLinkValues(input.id, index, parentTree)}
                            </p>
                        ),
                    },
                ],
            };
        }

        return input.inputs.reduce((acc, i) => {
            acc = buildInputTree(
                i,
                {
                    ...tree,
                    childNodes: [
                        ...(tree.childNodes ?? []),
                        {
                            id: input.id,
                            hasCaret: false,
                            isExpanded: true,
                            label: (
                                <p>
                                    <Tag backgroundColor={tagColor(operatorInputMapping[input.type]) as string}>
                                        {getOperatorLabel(input, operatorPlugins)}
                                    </Tag>
                                    <Spacing vertical size="tiny" />
                                    {getLinkValues(input.id, index, parentTree)}
                                </p>
                            ),
                        },
                    ],
                },
                index,
                tagInputTag,
                parentTree
            );
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
        (id: string, index: number, tree: TreeNodeInfo) => {
            const linkToValueMap = linksToValueMap[index];
            if (linksToValueMap.length && linkToValueMap) {
                return (
                    <TagList>
                        {linkToValueMap.get(id)?.value.map((val, i) => (
                            <React.Fragment key={val + i}>
                                <Tag
                                    round
                                    emphasis="stronger"
                                    interactive
                                    backgroundColor={
                                        val === treeValueQuery.get(index)
                                            ? "#746a85"
                                            : nodeParentHighlightedIds.get(index)?.includes(id)
                                            ? "#0097a7"
                                            : undefined
                                    }
                                    onMouseEnter={() => {
                                        handleValueHover("tree", val, index);
                                        handleParentNodeHighlights(tree, id, index);
                                    }}
                                    onMouseLeave={() => {
                                        handleValueHover("tree", "", index);
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

    const handleValueHover = React.useCallback((on: "table" | "tree", value: string, index) => {
        on === "table"
            ? setTreeValueQuery((prev) => new Map([...prev, [index, value]]))
            : setTableValueQuery((prev) => new Map([...prev, [index, value]]));
    }, []);

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
    }, []);
    return (
        <Grid>
            <GridRow>
                <GridColumn full>
                    <OverviewItem>
                        <OverviewItemLine>
                            <Switch
                                checked={showInputValues}
                                onChange={setShowInputValues}
                                label="always expand input values"
                            />
                            <Spacing vertical size="large" />
                            <Switch
                                checked={showOperators}
                                onChange={setShowOperators}
                                label="always expand operators"
                            />
                        </OverviewItemLine>
                        <Spacing vertical size="large" />
                        <OverviewItem>
                            <Card>
                                <OverviewItem hasSpacing>
                                    <OverviewItemDescription>
                                        <OverviewItemLine>
                                            <p>Sources/Targets/Links</p>
                                        </OverviewItemLine>
                                        <OverviewItemLine>
                                            <p>14,234/13,222/5,674</p>
                                        </OverviewItemLine>
                                    </OverviewItemDescription>
                                    <OverviewItemActions>
                                        <Icon name="item-info" tooltipText="evaluation statistics" />
                                    </OverviewItemActions>
                                </OverviewItem>
                            </Card>
                            <Spacing vertical />
                            <Card>
                                <OverviewItem hasSpacing>
                                    <TaskActivityWidget
                                        projectId={projectId}
                                        taskId={linkingTaskId}
                                        label="Evaluate Linking"
                                        activityName="EvaluateLinking"
                                        registerToReceiveUpdates={(status) =>
                                            setTaskEvaluationStatus(status.statusName)
                                        }
                                    />
                                </OverviewItem>
                            </Card>
                        </OverviewItem>
                    </OverviewItem>
                </GridColumn>
            </GridRow>
            <Spacing />
            <GridRow>
                <GridColumn full>
                    <SearchField value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
                </GridColumn>
            </GridRow>
            <Spacing />
            <GridRow>
                <GridColumn full>
                    {evaluationResults && evaluationResults.links.length && (
                        <DataTable rows={rowData} headers={headerData}>
                            {({ rows, headers, getHeaderProps, getTableProps }) => (
                                <TableContainer>
                                    <Table {...getTableProps()}>
                                        <TableHead>
                                            <TableRow>
                                                <TableHeader>
                                                    <IconButton
                                                        onClick={() => handleRowExpansion()}
                                                        name={
                                                            expandedRows.size === rowData.length
                                                                ? "toggler-showless"
                                                                : "toggler-showmore"
                                                        }
                                                    />
                                                </TableHeader>
                                                {headers.map((header) => (
                                                    <TableHeader
                                                        key={header.key}
                                                        {...getHeaderProps({ header, isSortable: true })}
                                                    >
                                                        {header.header}
                                                    </TableHeader>
                                                ))}
                                                <TableHeader>
                                                    <p>{t("linkingEvaluationTabView.table.header.score")}</p>
                                                </TableHeader>
                                                <TableHeader>
                                                    <OverviewItem>
                                                        <OverviewItemLine>
                                                            <p>
                                                                {t("linkingEvaluationTabView.table.header.linkState")}
                                                            </p>
                                                            <Spacing vertical size="tiny" />
                                                            <ContextMenu togglerElement="operation-filter">
                                                                <MenuItem
                                                                    data-test-id="search-item-copy-btn"
                                                                    key="copy"
                                                                    icon="state-confirmed"
                                                                    onClick={() => {}}
                                                                    text="Confirmed"
                                                                />
                                                                <MenuItem
                                                                    data-test-id="search-item-copy-btn"
                                                                    key="copy"
                                                                    icon="item-question"
                                                                    onClick={() => {}}
                                                                    text="Uncertain"
                                                                />
                                                                <MenuItem
                                                                    data-test-id="search-item-copy-btn"
                                                                    key="copy"
                                                                    icon="state-declined"
                                                                    onClick={() => {}}
                                                                    text="Declined"
                                                                />
                                                            </ContextMenu>
                                                        </OverviewItemLine>
                                                    </OverviewItem>
                                                </TableHeader>
                                            </TableRow>
                                        </TableHead>
                                        <TableBody>
                                            {rows.map((row, i) => {
                                                const currentInputValue = inputValues[i];
                                                const currentLink = evaluationResults?.links[i]!;

                                                return (
                                                    <>
                                                        {currentLink && (
                                                            <TableExpandRow
                                                                key={row.id}
                                                                isExpanded={expandedRows.has(row.id)}
                                                                onExpand={() => handleRowExpansion(row.id)}
                                                                ariaLabel="Links expansion"
                                                            >
                                                                {row.cells.map((cell) => (
                                                                    <TableCell key={cell.id}>{cell.value}</TableCell>
                                                                ))}
                                                                <TableCell>
                                                                    <ConfidenceValue value={currentLink.confidence} />
                                                                </TableCell>
                                                                <TableCell>
                                                                    <OverviewItem>
                                                                        <IconButton
                                                                            hasStateSuccess
                                                                            name="state-confirmed"
                                                                            active={currentLink.decision === "positive"}
                                                                        />
                                                                        <Spacing vertical size="tiny" />
                                                                        <IconButton
                                                                            name="item-question"
                                                                            active={
                                                                                currentLink.decision === "unlabeled"
                                                                            }
                                                                        />
                                                                        <Spacing vertical size="tiny" />
                                                                        <IconButton
                                                                            hasStateDanger
                                                                            name="state-declined"
                                                                            active={currentLink.decision === "negative"}
                                                                        />
                                                                    </OverviewItem>
                                                                </TableCell>
                                                            </TableExpandRow>
                                                        )}

                                                        {!!currentInputValue && (
                                                            <TableExpandedRow
                                                                colSpan={headers.length + 3}
                                                                className="linking-table__expanded-row-container"
                                                            >
                                                                <Grid>
                                                                    {showInputValues && (
                                                                        <GridRow>
                                                                            <span>
                                                                                <IconButton
                                                                                    onClick={() => {
                                                                                        setInputValuesExpansion(
                                                                                            (prevInputExpansion) => {
                                                                                                prevInputExpansion.set(
                                                                                                    i,
                                                                                                    !prevInputExpansion.get(
                                                                                                        i
                                                                                                    )
                                                                                                );
                                                                                                return new Map(
                                                                                                    prevInputExpansion
                                                                                                );
                                                                                            }
                                                                                        );
                                                                                    }}
                                                                                    name={
                                                                                        inputValuesExpansion.get(i)
                                                                                            ? "toggler-moveright"
                                                                                            : "toggler-showmore"
                                                                                    }
                                                                                />
                                                                            </span>
                                                                            <GridColumn full>
                                                                                <ComparisonDataContainer>
                                                                                    {Object.entries(
                                                                                        currentInputValue.source
                                                                                    ).map(([key, values]) => (
                                                                                        <ComparisonDataCell
                                                                                            fullWidth
                                                                                            className={
                                                                                                (inputValuesExpansion.get(
                                                                                                    i
                                                                                                ) &&
                                                                                                    "shrink") ||
                                                                                                ""
                                                                                            }
                                                                                        >
                                                                                            <PropertyBox
                                                                                                propertyName={key}
                                                                                                exampleValues={
                                                                                                    <ActiveLearningValueExamples
                                                                                                        interactive
                                                                                                        valuesToHighlight={
                                                                                                            new Set([
                                                                                                                tableValueQuery.get(
                                                                                                                    i
                                                                                                                ) ?? "",
                                                                                                            ])
                                                                                                        }
                                                                                                        onHover={(
                                                                                                            val
                                                                                                        ) =>
                                                                                                            handleValueHover(
                                                                                                                "table",
                                                                                                                val,
                                                                                                                i
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
                                                                            </GridColumn>
                                                                            <GridColumn full>
                                                                                {Object.entries(
                                                                                    currentInputValue.target
                                                                                ).map(([key, values]) => (
                                                                                    <ComparisonDataCell
                                                                                        fullWidth
                                                                                        className={
                                                                                            (inputValuesExpansion.get(
                                                                                                i
                                                                                            ) &&
                                                                                                "shrink") ||
                                                                                            ""
                                                                                        }
                                                                                    >
                                                                                        <PropertyBox
                                                                                            propertyName={key}
                                                                                            exampleValues={
                                                                                                <ActiveLearningValueExamples
                                                                                                    interactive
                                                                                                    valuesToHighlight={
                                                                                                        new Set([
                                                                                                            tableValueQuery.get(
                                                                                                                i
                                                                                                            ) ?? "",
                                                                                                        ])
                                                                                                    }
                                                                                                    onHover={(val) =>
                                                                                                        handleValueHover(
                                                                                                            "table",
                                                                                                            val,
                                                                                                            i
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
                                                                            </GridColumn>
                                                                        </GridRow>
                                                                    )}
                                                                    <Spacing size="tiny" />
                                                                    {showOperators && (
                                                                        <GridRow>
                                                                            <Tree
                                                                                contents={[nodes[i]]}
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
                                                                    )}
                                                                </Grid>
                                                            </TableExpandedRow>
                                                        )}
                                                    </>
                                                );
                                            })}
                                        </TableBody>
                                    </Table>
                                </TableContainer>
                            )}
                        </DataTable>
                    )}
                </GridColumn>
            </GridRow>
            <Spacing />
            <GridRow>
                <GridColumn>
                    <Pagination
                        pagination={pagination}
                        pageSizes={[10, 25, 50, 100]}
                        onChangeSelect={handlePagination}
                    />
                </GridColumn>
            </GridRow>
        </Grid>
    );
};

export default LinkingEvaluationTabView;