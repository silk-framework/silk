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
} from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import { TaskActivityWidget } from "../../../../../views/shared/TaskActivityWidget/TaskActivityWidget";
import Pagination from "../../../../../views/shared/Pagination";
import { getLinkingEvaluations, getLinkRuleInputPaths } from "./LinkingEvaluationViewUtils";
import { EvaluationLinkInputValue, LinkingEvaluationResult, LinkingEvaluationRule } from "./typings";
import utils from "../LinkingRuleEvaluation.utils";
import { ComparisonDataCell, ComparisonDataHeader } from "../../activeLearning/components/ComparisionData";
import { ActiveLearningValueExamples } from "../../activeLearning/shared/ActiveLearningValueExamples";
import { PropertyBox } from "../../activeLearning/components/PropertyBox";

interface LinkingEvaluationTabViewProps {
    projectId: string;
    linkingTaskId: string;
}

const defaultEvaluationResult = { links: [], linkRule: { operator: null } };

const LinkingEvaluationTabView: React.FC<LinkingEvaluationTabViewProps> = ({ projectId, linkingTaskId }) => {
    const [t] = useTranslation();
    const [evaluationResults, setEvaluationResults] =
        React.useState<{ links: Array<LinkingEvaluationResult>; linkRule: LinkingEvaluationRule }>(
            defaultEvaluationResult
        );
    const [pagination, setPagination] = React.useState<{ current: number; total: number; limit: number }>({
        current: 1,
        total: 25,
        limit: 10,
    });
    const [showInputValues, setShowInputValues] = React.useState<boolean>(true);
    const [showOperators, setShowOperators] = React.useState<boolean>(true);
    const [inputValues, setInputValues] = React.useState<Array<EvaluationLinkInputValue>>([]);
    const [expandedRows, setExpandedRows] = React.useState<Map<string, string>>(new Map());

    React.useEffect(() => {
        (async () => {
            setEvaluationResults(
                (await getLinkingEvaluations(projectId, linkingTaskId, pagination))?.data ?? defaultEvaluationResult
            );
        })();
    }, [pagination]);

    React.useEffect(() => {
        const ruleOperator = evaluationResults.linkRule.operator;
        if (ruleOperator) {
            const linkInputValues: Array<EvaluationLinkInputValue> = [];

            const inputPaths = ruleOperator.sourceInput
                ? getLinkRuleInputPaths(ruleOperator)
                : (ruleOperator.inputs ?? []).reduce(
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

            const linksToValueMap = evaluationResults.links.map((link) => utils.linkToValueMap(link as any));
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

            console.log({ inputPaths, linkInputValues });

            setInputValues(linkInputValues);
        }
    }, [evaluationResults.linkRule.operator]);

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
        {
            key: "confidence",
            header: t("linkingEvaluationTabView.table.header.score"),
        },
    ];

    const rowData = evaluationResults.links.map((evaluation, i) => ({ ...evaluation, id: `${i}` }));

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
                                                <TableHeader {...getHeaderProps({ header, isSortable: true })}>
                                                    {header.header}
                                                </TableHeader>
                                            ))}
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
                                            return (
                                                <>
                                                    <TableExpandRow
                                                        key={row.id}
                                                        isExpanded={expandedRows.has(row.id)}
                                                        onExpand={() => handleRowExpansion(row.id)}
                                                    >
                                                        {row.cells.map((cell) => (
                                                            <TableCell key={cell.id}>{cell.value}</TableCell>
                                                        ))}
                                                        <TableCell>
                                                            <OverviewItem>
                                                                <IconButton hasStateSuccess name="state-confirmed" />
                                                                <Spacing vertical size="tiny" />
                                                                <IconButton name="item-question" />
                                                                <Spacing vertical size="tiny" />
                                                                <IconButton hasStateDanger name="state-declined" />
                                                            </OverviewItem>
                                                        </TableCell>
                                                    </TableExpandRow>
                                                    <TableExpandedRow colSpan={headers.length + 3}>
                                                        <Grid>
                                                            <GridRow>
                                                                <GridColumn full>
                                                                    <ComparisonDataHeader fullWidth>
                                                                        {row.cells[0].value}
                                                                    </ComparisonDataHeader>
                                                                    {Object.entries(currentInputValue.source).map(
                                                                        ([key, values]) => (
                                                                            <ComparisonDataCell fullWidth>
                                                                                <PropertyBox
                                                                                    propertyName={key}
                                                                                    exampleValues={
                                                                                        <ActiveLearningValueExamples
                                                                                            exampleValues={values ?? []}
                                                                                        />
                                                                                    }
                                                                                />
                                                                            </ComparisonDataCell>
                                                                        )
                                                                    )}
                                                                </GridColumn>
                                                                <GridColumn full>
                                                                    <ComparisonDataHeader fullWidth>
                                                                        {row.cells[1].value}
                                                                    </ComparisonDataHeader>
                                                                    {Object.entries(currentInputValue.target).map(
                                                                        ([key, values]) => (
                                                                            <ComparisonDataCell fullWidth>
                                                                                <PropertyBox
                                                                                    propertyName={key}
                                                                                    exampleValues={
                                                                                        <ActiveLearningValueExamples
                                                                                            exampleValues={values ?? []}
                                                                                        />
                                                                                    }
                                                                                />
                                                                            </ComparisonDataCell>
                                                                        )
                                                                    )}
                                                                </GridColumn>
                                                            </GridRow>
                                                        </Grid>
                                                    </TableExpandedRow>
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
