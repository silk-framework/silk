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
import { getLinkingEvaluations, getLinkRuleInputPaths } from "./LinkingEvaluationViewUtils";
import { EvaluationLinkInputValue, LinkingEvaluationResult, NodePath } from "./typings";
import utils from "../LinkingRuleEvaluation.utils";
import {
    ComparisonDataCell,
    ComparisonDataContainer,
    ComparisonDataHeader,
} from "../../activeLearning/components/ComparisionData";
import { ActiveLearningValueExamples } from "../../activeLearning/shared/ActiveLearningValueExamples";
import { PropertyBox } from "../../activeLearning/components/PropertyBox";
import { IAggregationOperator, IComparisonOperator, ILinkingRule } from "../../linking.types";
import { TreeNodeInfo } from "@blueprintjs/core";
import { EvaluationResultType } from "../LinkingRuleEvaluation";
import { tagColor } from "../../../../../views/shared/RuleEditor/view/sidebar/RuleOperator";
import { addHighlighting } from "../../../../../views/shared/RuleEditor/view/ruleNode/ruleNode.utils";

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

    React.useEffect(() => {
        (async () => {
            if (taskEvaluationStatus === "Finished") {
                const results = (await getLinkingEvaluations(projectId, linkingTaskId, pagination))?.data;
                setEvaluationResults(results);
                setLinksToValueMap(results?.links.map((link) => utils.linkToValueMap(link as any)) ?? []);
                setInputValuesExpansion(() => new Map(results?.links.map((_, idx) => [idx, false])));
            }
        })();
    }, [pagination, taskEvaluationStatus]);

    React.useEffect(() => {
        if (!evaluationResults || !linksToValueMap.length) return;
        const operatorNode = evaluationResults?.linkRule.operator as any;
        setNodes(
            new Array(evaluationResults?.links.length).fill(1).map((_, idx) => {
                const treeInfo: TreeNodeInfo = {
                    id: operatorNode.id,
                    isExpanded: true,
                    label: (
                        <span>
                            {operatorNode.type}:
                            {operatorNode.type === "Aggregation" ? operatorNode.aggregator : operatorNode.metric} (
                            {operatorNode.id})
                            <Spacing vertical size="tiny" />
                            <ConfidenceValue value={operatorNode.weight} spaceUsage="minimal" />
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
                                    {operatorInputMapping[node[inputPath].type] ?? node[inputPath].type}:
                                    {node[inputPath].function ?? node[inputPath].path ?? ""} ({node[inputPath].id}){" "}
                                    {getLinkValues(
                                        node[inputPath].id,
                                        idx,
                                        tagColor(
                                            node[inputPath].type === "pathInput"
                                                ? inputPathCategory[inputPath]
                                                : operatorInputMapping[node[inputPath].type]
                                        ) as string
                                    )}
                                </p>
                            ),
                            childNodes: [],
                        };

                        if (node[inputPath].inputs?.length) {
                            node[inputPath].inputs.forEach((i) => {
                                inputNode = buildInputTree(i, inputNode, idx, inputPathCategory[inputPath]);
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
                                    {i.type}: {(i as IComparisonOperator).metric} ({i.id})
                                    <Spacing vertical size="tiny" />
                                    <ConfidenceValue value={i.weight} spaceUsage="minimal" />
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
    }, [evaluationResults, linksToValueMap, pagination]);

    React.useEffect(() => {
        if (evaluationResults && evaluationResults.linkRule && evaluationResults.links && linksToValueMap.length) {
            const ruleOperator = evaluationResults.linkRule.operator;
            if (ruleOperator) {
                const linkInputValues: Array<EvaluationLinkInputValue> = [];

                const inputPaths = (ruleOperator as IComparisonOperator)?.sourceInput
                    ? getLinkRuleInputPaths(ruleOperator)
                    : ((ruleOperator as IAggregationOperator)?.inputs ?? []).reduce(
                          (inputPaths, input) => {
                              const linkRuleInputPaths = getLinkRuleInputPaths(input);
                              inputPaths = {
                                  source: {
                                      ...inputPaths.source,
                                      ...linkRuleInputPaths.source,
                                  },
                                  target: {
                                      ...inputPaths.target,
                                      ...linkRuleInputPaths.target,
                                  },
                              };
                              return inputPaths;
                          },
                          { source: {}, target: {} } as EvaluationLinkInputValue
                      );

                linksToValueMap.forEach((linkToValueMap) => {
                    const matchingInputValue: EvaluationLinkInputValue = { source: {}, target: {} };
                    Object.entries(inputPaths.source).forEach(([uri, operatorIds]) => {
                        matchingInputValue.source[uri] = operatorIds
                            .map((id) => linkToValueMap.get(id)?.value ?? [])
                            .flat();
                    });

                    Object.entries(inputPaths.target).forEach(([uri, operatorIds]) => {
                        matchingInputValue.target[uri] = operatorIds
                            .map((id) => linkToValueMap.get(id)?.value ?? [])
                            .flat();
                    });

                    linkInputValues.push(matchingInputValue);
                });
                setInputValues(linkInputValues);
            }
        }
    }, [evaluationResults, linksToValueMap, pagination]);

    const buildInputTree = (input: any, tree: TreeNodeInfo, index: number, tagInputTag: string): TreeNodeInfo => {
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
                                {operatorInputMapping[input.type]}:{input.path}({input.id}){" "}
                                {getLinkValues(input.id, index, tagColor(tagInputTag) as string)}
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
                                    {operatorInputMapping[input.type]}:{input.function}({input.id}){" "}
                                    {getLinkValues(
                                        input.id,
                                        index,
                                        tagColor(operatorInputMapping[input.type]) as string
                                    )}
                                </p>
                            ),
                        },
                    ],
                },
                index,
                tagInputTag
            );
            return acc;
        }, {} as TreeNodeInfo);
    };

    const getLinkValues = React.useCallback(
        (id: string, index: number, tagColor?: string, query = "") => {
            if (linksToValueMap.length) {
                const linkToValueMap = linksToValueMap[index];
                return (
                    <TagList>
                        {linkToValueMap.get(id)?.value.map((val, i) => (
                            <React.Fragment key={val + i}>
                                <Tag
                                    backgroundColor={tagColor}
                                    interactive
                                    onMouseEnter={() => handleValueHover("tree", val, index)}
                                    onMouseLeave={() => handleValueHover("tree", "", index)}
                                >
                                    {addHighlighting(val, query)}
                                </Tag>
                                <Spacing vertical size="tiny" />
                            </React.Fragment>
                        ))}
                    </TagList>
                );
            }
        },
        [linksToValueMap]
    );

    const handleValueHover = React.useCallback((on: "table" | "tree", value: string, index) => {
        on === "table"
            ? setTreeValueQuery((prev) => new Map([...prev, [index, value]]))
            : setTableValueQuery((prev) => new Map([...prev, [index, value]]));
    }, []);

    const handlePagination = React.useCallback((page: number, limit: number) => {
        setPagination({ current: 1, total: 25, limit });
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
                            <Switch checked={showInputValues} onChange={setShowInputValues} label="Show input values" />
                            <Spacing vertical size="large" />
                            <Switch checked={showOperators} onChange={setShowOperators} label="Show operators" />
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
                    <SearchField />
                </GridColumn>
            </GridRow>
            <Spacing />
            <GridRow>
                <GridColumn full>
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
                                                        <p>{t("linkingEvaluationTabView.table.header.linkState")}</p>
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
                                            const { decision, confidence } = evaluationResults?.links[i]!;
                                            return (
                                                <>
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
                                                            <ConfidenceValue value={confidence} />
                                                        </TableCell>
                                                        <TableCell>
                                                            <OverviewItem>
                                                                <IconButton
                                                                    hasStateSuccess
                                                                    name="state-confirmed"
                                                                    active={decision === "positive"}
                                                                />
                                                                <Spacing vertical size="tiny" />
                                                                <IconButton
                                                                    name="item-question"
                                                                    active={decision === "unlabeled"}
                                                                />
                                                                <Spacing vertical size="tiny" />
                                                                <IconButton
                                                                    hasStateDanger
                                                                    name="state-declined"
                                                                    active={decision === "negative"}
                                                                />
                                                            </OverviewItem>
                                                        </TableCell>
                                                    </TableExpandRow>
                                                    {!!currentInputValue && (
                                                        <TableExpandedRow
                                                            colSpan={headers.length + 3}
                                                            className="linking-table__expanded-row-container"
                                                        >
                                                            <Grid>
                                                                <GridRow>
                                                                    <span>
                                                                        <IconButton
                                                                            onClick={() => {
                                                                                setInputValuesExpansion(
                                                                                    (prevInputExpansion) => {
                                                                                        prevInputExpansion.set(
                                                                                            i,
                                                                                            !prevInputExpansion.get(i)
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
                                                                            {/* <ComparisonDataHeader fullWidth>
                                                                                {row.cells[0].value}
                                                                            </ComparisonDataHeader> */}
                                                                            {Object.entries(
                                                                                currentInputValue.source
                                                                            ).map(([key, values]) => (
                                                                                <ComparisonDataCell
                                                                                    fullWidth
                                                                                    className={
                                                                                        (inputValuesExpansion.get(i) &&
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
                                                                        </ComparisonDataContainer>
                                                                    </GridColumn>
                                                                    <GridColumn full>
                                                                        {/* <ComparisonDataHeader fullWidth>
                                                                            {row.cells[1].value}
                                                                        </ComparisonDataHeader> */}
                                                                        {Object.entries(currentInputValue.target).map(
                                                                            ([key, values]) => (
                                                                                <ComparisonDataCell
                                                                                    fullWidth
                                                                                    className={
                                                                                        (inputValuesExpansion.get(i) &&
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
                                                                            )
                                                                        )}
                                                                    </GridColumn>
                                                                </GridRow>
                                                                <Spacing size="tiny" />
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
