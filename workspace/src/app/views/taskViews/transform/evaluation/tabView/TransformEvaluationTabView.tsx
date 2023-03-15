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
} from "@eccenca/gui-elements";
import { DataTableCustomRenderProps } from "carbon-components-react";
import React from "react";
import { useTranslation } from "react-i18next";
import MappingsTree from "../../../../../views/pages/MappingEditor/HierarchicalMapping/containers/MappingsTree";
import { getEvaluatedEntities } from "./TransformEvaluationTabViewUtils";
import { EvaluatedRuleEntityResult } from "./typing";

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
    const [evaluatedEntityResults, setEvaluatedEntityResults] = React.useState<EvaluatedRuleEntityResult | undefined>(); //Todo use "useRef" when perf issues start to happen
    const [expandedRows] = React.useState<Map<number, number>>(new Map());
    const [currentRuleId, setCurrentRuleId] = React.useState<string>("root");
    const [t] = useTranslation();

    React.useEffect(() => {
        (async () => {
            const results = await (
                await getEvaluatedEntities(projectId, transformTaskId, currentRuleId, 10, false)
            ).data;
            setEvaluatedEntityResults(results);
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
            evaluatedEntityResults?.evaluatedEntities.map((entity, i) => ({
                uri: entity.uris[0],
                id: `${i}`,
            })) ?? [],
        [evaluatedEntityResults]
    );

    const handleRowExpansion = React.useCallback((rowIdx?: number) => () => {}, []);

    return (
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
                                <TableBody>
                                    {rows.map((rowItem, rowIdx) => {
                                        return (
                                            <TableExpandRow
                                                key={rowItem.id}
                                                isExpanded={expandedRows.has(rowIdx)}
                                                onExpand={handleRowExpansion(rowIdx)}
                                                togglerText={
                                                    expandedRows.has(rowIdx)
                                                        ? t("linkingEvaluationTabView.table.collapseRow")
                                                        : t("linkingEvaluationTabView.table.expandRow")
                                                }
                                            >
                                                <TableCell>{rowItem.uri}</TableCell>
                                            </TableExpandRow>
                                        );
                                    })}
                                </TableBody>
                            </Table>
                        )}
                    </TableContainer>
                </GridColumn>
            </GridRow>
        </Grid>
    );
};

export default TransformEvaluationTabView;
