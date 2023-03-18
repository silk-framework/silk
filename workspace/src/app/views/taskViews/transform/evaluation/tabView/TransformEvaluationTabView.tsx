import {
    Grid,
    GridColumn,
    GridRow,
    TableBody,
    TableHeader,
    Table,
    TableHead,
    TableRow,
    TableContainer,
    TableExpandHeader,
    TableExpandRow,
    TableCell,
    Spinner,
    Notification,
    Spacing,
    Section,
    TreeNodeInfo,
    TableExpandedRow,
    IconButton,
    Tree,
} from "@eccenca/gui-elements";
import { DataTableCustomRenderProps } from "carbon-components-react";
import React from "react";
import { useTranslation } from "react-i18next";
import MappingsTree from "../../../../../views/pages/MappingEditor/HierarchicalMapping/containers/MappingsTree";
import { getEvaluatedEntities } from "./TransformEvaluationTabViewUtils";
import { EvaluatedComplexRule, EvaluatedEntityOperator, EvaluatedRuleEntityResult, EvaluatedURIRule } from "./typing";

interface TransformEvaluationTabViewProps {
    projectId: string;
    transformTaskId: string;
    startFullScreen: boolean;
}

const TransformEvaluationTabView: React.FC<TransformEvaluationTabViewProps> = ({
    projectId,
    transformTaskId,
    startFullScreen,
}) => {
    const evaluatedEntityResults = React.useRef<EvaluatedRuleEntityResult | undefined>();
    const [expandedRows] = React.useState<Map<number, number>>(new Map());
    const [loading, setLoading] = React.useState<boolean>(false);
    const [currentRuleId, setCurrentRuleId] = React.useState<string>("root");
    const nodes = React.useRef<TreeNodeInfo[][]>([]);
    const [t] = useTranslation();

    React.useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const results = await (
                    await getEvaluatedEntities(projectId, transformTaskId, currentRuleId, 10, false)
                ).data;
                evaluatedEntityResults.current = results;
            } catch (err) {
            } finally {
                setLoading(false);
            }
        })();
    }, [currentRuleId]);

    const handleRuleNavigation = React.useCallback(({ newRuleId }) => {
        setCurrentRuleId(newRuleId);
    }, []);

    const headers = React.useRef([
        {
            key: "TransformEntities",
            header: "Transform Entities", //Todo use translation
        },
    ]).current;

    React.useEffect(() => {
        if (evaluatedEntityResults.current) {
            const { rules, evaluatedEntities } = evaluatedEntityResults.current;
            nodes.current = evaluatedEntities.map((entity) =>
                entity.values.map((e) => {
                    const matchingRuleType = rules.find((rule) => rule.operator.id === e.operatorId)!;
                    const newNode = (
                        rule: EvaluatedURIRule["operator"],
                        values: EvaluatedEntityOperator["values"]
                    ): TreeNodeInfo<{}> => {
                        return {
                            id: rule.id,
                            hasCaret: false,
                            isExpanded: true,
                            label: (
                                <span>
                                    <>
                                        {rule.type} ({rule.id ?? ""})
                                    </>
                                    <Spacing vertical size="tiny" />
                                    <>
                                        {values.map((v, i) => (
                                            <span key={i}>{v}</span>
                                        ))}
                                    </>
                                </span>
                            ),
                        };
                    };
                    let treeNodeInfo = {
                        hasCaret: false,
                        id: matchingRuleType.id,
                        isExpanded: true,
                        label: <p>{(matchingRuleType as EvaluatedComplexRule)?.mappingTarget?.uri ?? "URI"}</p>,
                        childNodes: [],
                    } as TreeNodeInfo;

                    const generateTree = (
                        rule: EvaluatedURIRule["operator"],
                        entityValue: EvaluatedEntityOperator,
                        tree: TreeNodeInfo
                    ) => {
                        if (!rule.inputs?.length) {
                            tree.childNodes = [...(tree?.childNodes ?? []), newNode(rule, entityValue.values)];
                            return tree;
                        }
                        const currentNode = newNode(rule, entityValue.values);
                        tree.childNodes = [...(tree?.childNodes ?? []), currentNode];

                        for (let i = 0; i < rule.inputs.length; i++) {
                            generateTree(rule.inputs[i], entityValue.children[i], currentNode);
                        }
                    };

                    generateTree(matchingRuleType.operator, e, treeNodeInfo);
                    return treeNodeInfo;
                })
            );
        }
    }, [evaluatedEntityResults.current]);

    const rows = React.useMemo(
        () =>
            evaluatedEntityResults.current?.evaluatedEntities.map((entity, i) => ({
                uri: entity.uris[0],
                id: `${i}`,
            })) ?? [],
        [evaluatedEntityResults.current]
    );

    const handleRowExpansion = React.useCallback((rowIdx?: number) => () => {}, []);
    /**
     * todo ui issues
     *  1. overflowing ui vertically and horizontally
     *  2. table overlaps with mappingTree
     *  3. table needs padding to the right
     */
    return (
        <Section>
            <Grid useAbsoluteSpace className="transform-evaluation">
                <GridRow fullHeight>
                    <GridColumn small>
                        <MappingsTree
                            currentRuleId={currentRuleId}
                            handleRuleNavigation={handleRuleNavigation}
                            startFullScreen={startFullScreen}
                        />
                    </GridColumn>
                    <GridColumn>
                        <TableContainer rows={rows} headers={headers}>
                            {({ getTableProps }: DataTableCustomRenderProps) => (
                                <Table {...getTableProps()} size="compact" useZebraStyles>
                                    <TableHead>
                                        <TableRow>
                                            <TableExpandHeader
                                                enableToggle
                                                isExpanded={expandedRows.size === rows.length}
                                                onExpand={handleRowExpansion()}
                                                togglerText={
                                                    expandedRows.size === rows.length
                                                        ? t("linkingEvaluationTabView.table.header.collapseRows")
                                                        : t("linkingEvaluationTabView.table.header.expandRows")
                                                }
                                            />
                                            <TableHeader>{headers[0].header}</TableHeader>
                                        </TableRow>
                                    </TableHead>
                                    {(!loading && rows.length && (
                                        <TableBody>
                                            {rows.map((rowItem, rowIdx) => {
                                                return (
                                                    <React.Fragment key={rowItem.id}>
                                                        <TableExpandRow
                                                            isExpanded={true}
                                                            onExpand={handleRowExpansion(rowIdx)}
                                                            togglerText={
                                                                expandedRows.has(rowIdx)
                                                                    ? t("linkingEvaluationTabView.table.collapseRow")
                                                                    : t("linkingEvaluationTabView.table.expandRow")
                                                            }
                                                        >
                                                            <TableCell>{rowItem.uri}</TableCell>
                                                        </TableExpandRow>
                                                        <TableExpandedRow
                                                            colSpan={headers.length * 2}
                                                            className="linking-table__expanded-row-container"
                                                        >
                                                            <Table size="compact" hasDivider={false} colorless>
                                                                <TableBody>
                                                                    <TableRow>
                                                                        <TableCell
                                                                            style={{
                                                                                paddingLeft: "0",
                                                                                paddingRight: "0",
                                                                            }}
                                                                        >
                                                                            <IconButton
                                                                                data-test-id="tree-expand-item-btn"
                                                                                id={`tree-btn-${
                                                                                    true ? "expanded" : "collapsed"
                                                                                }`}
                                                                                onClick={() => {}}
                                                                                name={
                                                                                    true
                                                                                        ? "toggler-caretright"
                                                                                        : "toggler-caretdown"
                                                                                }
                                                                            />
                                                                        </TableCell>
                                                                        <TableCell>
                                                                            <Tree
                                                                                contents={nodes.current[rowIdx] ?? []}
                                                                            />
                                                                        </TableCell>
                                                                    </TableRow>
                                                                </TableBody>
                                                            </Table>
                                                        </TableExpandedRow>
                                                    </React.Fragment>
                                                );
                                            })}
                                        </TableBody>
                                    )) ||
                                        (loading && <Spinner size="small" />) || (
                                            <>
                                                <Spacing />
                                                <Notification warning data-test-id="notification-unknown-problem">
                                                    {t("linkingEvaluationTabView.messages.unknownProblem")}
                                                </Notification>
                                            </>
                                        )}
                                </Table>
                            )}
                        </TableContainer>
                    </GridColumn>
                </GridRow>
            </Grid>
        </Section>
    );
};

export default TransformEvaluationTabView;
