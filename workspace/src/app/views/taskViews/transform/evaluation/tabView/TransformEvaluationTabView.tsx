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
    Spinner,
    Notification,
    Spacing,
    Section,
    TreeNodeInfo,
    TagList,
    Tag,
} from "@eccenca/gui-elements";
import { DataTableCustomRenderProps } from "carbon-components-react";
import React from "react";
import { useTranslation } from "react-i18next";
import { OperatorLabel } from "../../../../../views/taskViews/shared/evaluations/OperatorLabel";
import MappingsTree from "../../../../../views/pages/MappingEditor/HierarchicalMapping/containers/MappingsTree";
import { getEvaluatedEntities } from "./TransformEvaluationTabViewUtils";
import { EvaluatedComplexRule, EvaluatedEntityOperator, EvaluatedRuleEntityResult, EvaluatedURIRule } from "./typing";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import { operatorInputMapping } from "../../../../../views/taskViews/linking/evaluation/tabView/LinkingEvaluationRow";
import TransformEvaluationTabRow from "./TransformEvaluationTabRow";

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
    const [allRowsExpanded, setAllRowsExpanded] = React.useState<boolean>(false);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [currentRuleId, setCurrentRuleId] = React.useState<string>("root");
    const operatorPlugins = React.useRef<Array<IPluginDetails>>([]);
    const nodes = React.useRef<TreeNodeInfo[][]>([]);
    const [t] = useTranslation();

    React.useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const [results, plugins] = await Promise.all([
                    (await getEvaluatedEntities(projectId, transformTaskId, currentRuleId, 10, false)).data,
                    Object.values((await requestRuleOperatorPluginDetails(false)).data),
                ]);
                operatorPlugins.current = plugins;
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
        if (evaluatedEntityResults.current && operatorPlugins.current?.length) {
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
                                <OperatorLabel
                                    tagPluginType={operatorInputMapping[rule.type]}
                                    operator={rule}
                                    operatorPlugins={operatorPlugins.current}
                                >
                                    <TagList>
                                        {values.map((v, i) => (
                                            <Tag key={i} round emphasis="stronger" interactive>
                                                {v}
                                            </Tag>
                                        ))}
                                    </TagList>
                                </OperatorLabel>
                            ),
                        };
                    };
                    let treeNodeInfo = {
                        hasCaret: true,
                        id: matchingRuleType.id,
                        isExpanded: true,
                        label: (
                            <strong>{(matchingRuleType as EvaluatedComplexRule)?.mappingTarget?.uri ?? "URI"}</strong>
                        ),
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
    }, [evaluatedEntityResults.current, operatorPlugins.current]);

    const rows = React.useMemo(
        () =>
            evaluatedEntityResults.current?.evaluatedEntities.map((entity, i) => ({
                uri: entity.uris[0],
                id: `${i}`,
            })) ?? [],
        [evaluatedEntityResults.current]
    );

    const expandAllRows = React.useCallback(() => {
        setAllRowsExpanded((e) => !e);
    }, []);
    /**
     * todo ui issues
     *  1. overflowing ui vertically and horizontally
     *  2. table overlaps with mappingTree
     *  3. table needs padding to the right
     */
    return (
        <Section className="diapp-linking-evaluation">
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
                                                isExpanded={allRowsExpanded}
                                                onExpand={expandAllRows}
                                                togglerText={
                                                    allRowsExpanded
                                                        ? t("linkingEvaluationTabView.table.header.collapseRows")
                                                        : t("linkingEvaluationTabView.table.header.expandRows")
                                                }
                                            />
                                            <TableHeader>{headers[0].header}</TableHeader>
                                        </TableRow>
                                    </TableHead>
                                    {(!loading &&
                                        rows.length &&
                                        evaluatedEntityResults.current &&
                                        evaluatedEntityResults.current.evaluatedEntities.length && (
                                            <TableBody>
                                                {rows.map((rowItem, rowIdx) => (
                                                    <TransformEvaluationTabRow
                                                        rowExpandedByParent={allRowsExpanded}
                                                        rowItem={rowItem}
                                                        colSpan={headers.length * 2}
                                                        operatorPlugins={operatorPlugins.current}
                                                        entity={
                                                            evaluatedEntityResults.current!.evaluatedEntities[rowIdx]
                                                        }
                                                        rules={evaluatedEntityResults.current!.rules}
                                                    />
                                                ))}
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
