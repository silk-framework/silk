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
} from "@eccenca/gui-elements";
import { DataTableCustomRenderProps } from "carbon-components-react";
import React from "react";
import { useTranslation } from "react-i18next";
import MappingsTree from "../../../../../views/pages/MappingEditor/HierarchicalMapping/containers/MappingsTree";
import { getEvaluatedEntities } from "./TransformEvaluationTabViewUtils";
import { EvaluatedRuleEntityResult } from "./typing";
import { requestRuleOperatorPluginDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
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
