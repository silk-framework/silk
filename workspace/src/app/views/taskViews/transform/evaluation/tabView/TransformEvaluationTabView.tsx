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
} from "@eccenca/gui-elements";
import { DataTableCustomRenderProps } from "carbon-components-react";
import React from "react";
import { useTranslation } from "react-i18next";
import { getEvaluatedEntities } from "./TransformEvaluationTabViewUtils";
import { EvaluatedRuleEntityResult } from "./typing";
import { requestRuleOperatorPluginsDetails } from "@ducks/common/requests";
import { IPluginDetails } from "@ducks/common/typings";
import TransformEvaluationTabRow from "./TransformEvaluationTabRow";
import MappingsTree from "../../../../pages/MappingEditor/HierarchicalMapping/containers/MappingsTree";
import { IViewActions } from "../../../../../views/plugins/PluginRegistry";

interface TransformEvaluationTabViewProps {
    projectId: string;
    transformTaskId: string;
    startFullScreen: boolean;
    viewActions?: IViewActions;
}

const TransformEvaluationTabView: React.FC<TransformEvaluationTabViewProps> = ({
    projectId,
    transformTaskId,
    startFullScreen,
    viewActions,
}) => {
    const evaluatedEntityResults = React.useRef<EvaluatedRuleEntityResult | undefined>();
    const [allRowsExpanded, setAllRowsExpanded] = React.useState<boolean>(false);
    const [loading, setLoading] = React.useState<boolean>(false);
    const [currentRuleId, setCurrentRuleId] = React.useState<string>(() => {
        const ruleId = new URLSearchParams(window.location.search).get("ruleId");
        return ruleId ?? "root";
    });
    const operatorPlugins = React.useRef<Array<IPluginDetails>>([]);
    const [error, setError] = React.useState<string>("");
    const [expandRowTrees, setExpandRowTrees] = React.useState<boolean>(false);
    const [t] = useTranslation();

    React.useEffect(() => {
        (async () => {
            try {
                setLoading(true);
                const [results, plugins] = await Promise.all([
                    (await getEvaluatedEntities(projectId, transformTaskId, currentRuleId, 10, true)).data,
                    Object.values((await requestRuleOperatorPluginsDetails(false)).data),
                ]);
                operatorPlugins.current = plugins;
                evaluatedEntityResults.current = results;
            } catch (err) {
                evaluatedEntityResults.current = undefined;
                setError(err?.body.detail ?? "");
            } finally {
                setLoading(false);
            }
        })();
        if (viewActions?.addLocalBreadcrumbs) {
            viewActions.addLocalBreadcrumbs([]);
        }
    }, [currentRuleId]);

    const handleRuleNavigation = React.useCallback(({ newRuleId }) => {
        setCurrentRuleId(newRuleId);
    }, []);

    const headers = React.useRef([
        {
            key: "TransformEntities",
            header: t("transformEvaluationTabView.table.header.transformEntities"),
        },
    ]).current;

    const rows = React.useMemo(
        () =>
            evaluatedEntityResults.current?.evaluatedEntities.map((entity, i) => ({
                uri: entity.uris[0],
                id: `id_${i}`,
            })) ?? [],
        [evaluatedEntityResults.current]
    );

    const expandAllRows = React.useCallback(() => {
        setAllRowsExpanded((e) => {
            if (e && !expandRowTrees) {
                //already expanded for first level
                setExpandRowTrees(true);
                return e;
            }
            setExpandRowTrees(false);
            return !e;
        });
    }, [expandRowTrees]);
    /**
     * todo ui issues
     *  1. overflowing ui vertically and horizontally
     *  3. table needs padding to the right
     */
    return (
        <section className="diapp-transform-evaluation">
            <Grid>
                <GridRow>
                    <GridColumn medium>
                        <MappingsTree
                            currentRuleId={currentRuleId}
                            handleRuleNavigation={handleRuleNavigation}
                            startFullScreen={startFullScreen}
                            trackRuleInUrl
                        />
                    </GridColumn>
                    <GridColumn className="diapp-linking-evaluation">
                        <TableContainer rows={rows} headers={headers}>
                            {({ getTableProps }: DataTableCustomRenderProps) => (
                                <Table {...getTableProps()} size={"medium"}>
                                    <TableHead>
                                        <TableRow>
                                            <TableExpandHeader
                                                enableToggle
                                                isExpanded={allRowsExpanded}
                                                onExpand={expandAllRows}
                                                togglerText={
                                                    allRowsExpanded && expandRowTrees
                                                        ? t("linkingEvaluationTabView.table.header.collapseRows")
                                                        : allRowsExpanded && !expandRowTrees
                                                        ? t("linkingEvaluationTabView.table.header.expandTrees")
                                                        : t("linkingEvaluationTabView.table.header.expandRows")
                                                }
                                                toggleIcon={
                                                    allRowsExpanded && !expandRowTrees ? "toggler-rowexpand" : undefined
                                                }
                                            />
                                            <TableHeader>{headers[0].header}</TableHeader>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {(!loading &&
                                            rows.length &&
                                            evaluatedEntityResults.current &&
                                            evaluatedEntityResults.current.evaluatedEntities.length && (
                                                <>
                                                    {rows.map((rowItem, rowIdx) => (
                                                        <TransformEvaluationTabRow
                                                            zebra={rowIdx % 2 === 1}
                                                            expandRowTrees={expandRowTrees}
                                                            key={rowIdx}
                                                            rowExpandedByParent={allRowsExpanded}
                                                            rowItem={rowItem}
                                                            colSpan={headers.length * 2}
                                                            operatorPlugins={operatorPlugins.current}
                                                            entity={
                                                                evaluatedEntityResults.current!.evaluatedEntities[
                                                                    rowIdx
                                                                ]
                                                            }
                                                            rules={evaluatedEntityResults.current!.rules}
                                                        />
                                                    ))}
                                                </>
                                            )) ||
                                            (loading && (
                                                <tr>
                                                    <td colSpan={2}>
                                                        <Spinner size="small" />
                                                    </td>
                                                </tr>
                                            )) || (
                                                <tr>
                                                    <td colSpan={2}>
                                                        <Spacing />
                                                        <Notification
                                                            warning={true}
                                                            data-test-id="notification-unknown-problem"
                                                        >
                                                            {t("transformEvaluationTabView.couldNotLoad")} {error}
                                                        </Notification>
                                                    </td>
                                                </tr>
                                            )}
                                    </TableBody>
                                </Table>
                            )}
                        </TableContainer>
                    </GridColumn>
                </GridRow>
            </Grid>
        </section>
    );
};

export default TransformEvaluationTabView;
