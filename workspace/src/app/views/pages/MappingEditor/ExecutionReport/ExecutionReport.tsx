import React from "react";
import PropTypes from "prop-types";
import {
    Grid,
    GridRow,
    GridColumn,
    Notification,
    HtmlContentBlock,
    Section,
    Spacing,
    PropertyValueList,
    PropertyValuePair,
    PropertyName,
    PropertyValue,
    Table,
    TableHeader,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
} from "@eccenca/gui-elements";
import MappingsTree from "../HierarchicalMapping/containers/MappingsTree";
import { SampleError } from "../../../shared/SampleError/SampleError";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../../plugins/PluginRegistry";
import { DataPreviewProps } from "../../../plugins/plugin.types";
import { ExecutionReportResponse, OutputEntitiesSample } from "./report-typings";
import { useTranslation } from "react-i18next";

interface ExecutionReportProps {
    /** The execution report to render. */
    executionReport?: ExecutionReportResponse;
    /** Optional execution meta-data that includes start time, user, etc. */
    executionMetaData?: any;
    trackRuleInUrl?: boolean;
}

/**
 * Displays a task execution report.
 */
export const ExecutionReport = ({ executionReport, executionMetaData, trackRuleInUrl }: ExecutionReportProps) => {
    const [currentRuleId, setCurrentRuleId] = React.useState<string | null>(null);
    const [t] = useTranslation();

    React.useEffect(() => {
        const ruleResults = executionReport?.ruleResults;
        if (ruleResults) {
            const initialRuleId = new URLSearchParams(window.location.search).get("ruleId");
            if (initialRuleId && ruleResults[initialRuleId]) {
                onRuleNavigation({ newRuleId: initialRuleId });
            }
        }
    }, [executionReport?.ruleResults]);

    const onRuleNavigation = ({ newRuleId }: { newRuleId: string }) => {
        setCurrentRuleId(newRuleId);
    };

    const renderSummary = () => {
        let title;
        if (executionReport?.entityCount != null && executionReport?.operationDesc != null) {
            title =
                `${t("ExecutionReport.execution")}: ` +
                executionReport.entityCount +
                " " +
                executionReport.operationDesc;
        } else {
            title = t("ExecutionReport.defaultTitle");
        }

        let executionMetaDataPairs: JSX.Element[] = [];
        if (executionMetaData != null) {
            executionMetaDataPairs = executionMetaDataPairs.concat([
                <PropertyValuePair hasDivider key="queuedAt">
                    <PropertyName className="silk-report-table-bold">Queued at</PropertyName>
                    <PropertyValue>
                        {executionMetaData.queuedAt == null
                            ? t("common.messages.notAvailable")
                            : executionMetaData.queuedAt}
                    </PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="startedAt">
                    <PropertyName className="silk-report-table-bold">Started at</PropertyName>
                    <PropertyValue>
                        {executionMetaData.startedAt == null
                            ? t("common.messages.notAvailable")
                            : executionMetaData.startedAt}
                    </PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="startedByUser">
                    <PropertyName className="silk-report-table-bold">Started by</PropertyName>
                    <PropertyValue>
                        {executionMetaData.startedByUser == null
                            ? t("common.words.unknown")
                            : executionMetaData.startedByUser}
                    </PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="finishedAt">
                    <PropertyName className="silk-report-table-bold">Finished at</PropertyName>
                    <PropertyValue>{executionMetaData.finishedAt}</PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="finishStatus">
                    <PropertyName className="silk-report-table-bold">Finish status</PropertyName>
                    <PropertyValue>{executionMetaData.finishStatus.message}</PropertyValue>
                </PropertyValuePair>,
            ]);
            if (executionMetaData.cancelledAt != null) {
                executionMetaDataPairs.push(
                    <PropertyValuePair hasDivider key="cancelledAt">
                        <PropertyName className="silk-report-table-bold">Cancelled at</PropertyName>
                        <PropertyValue>{executionMetaData.cancelledAt}</PropertyValue>
                    </PropertyValuePair>
                );
            }
            if (executionMetaData.cancelledBy != null) {
                executionMetaDataPairs.push(
                    <PropertyValuePair hasDivider key="cancelledBy">
                        <PropertyName className="silk-report-table-bold">Cancelled by</PropertyName>
                        <PropertyValue>{executionMetaData.cancelledBy}</PropertyValue>
                    </PropertyValuePair>
                );
            }
        }

        const summaryRows = executionReport
            ? executionReport.summary.map((v) => (
                  <PropertyValuePair hasDivider key={v.key}>
                      <PropertyName className="silk-report-table-bold">{v.key}</PropertyName>
                      <PropertyValue>{v.value}</PropertyValue>
                  </PropertyValuePair>
              ))
            : null;

        return (
            <Section className="silk-report-card">
                {renderWarning()}
                <Notification>
                    <p>{title}</p>
                    <PropertyValueList className="silk-report-table">
                        {executionMetaDataPairs}
                        {summaryRows}
                    </PropertyValueList>
                </Notification>
                <Spacing size="tiny" />
            </Section>
        );
    };

    const renderWarning = () => {
        let messages: string[] = [];
        let notificationState = "success";
        if (executionReport) {
            const label = executionReport.label;
            if (executionMetaData != null && executionMetaData.finishStatus.cancelled) {
                messages = [t("ExecutionReport.statusMessages.taskCancelled", { label })];
                notificationState = "warning";
            } else if (executionReport.error != null) {
                messages = [t("ExecutionReport.statusMessages.taskError", { label, error: executionReport.error })];
                notificationState = "danger";
            } else if (executionMetaData != null && executionMetaData.finishStatus.failed) {
                messages = [t("ExecutionReport.statusMessages.taskFailed", { label })];
                notificationState = "danger";
            } else if (executionReport.warnings.length > 0) {
                messages = executionReport.warnings;
                notificationState = "neutral";
            } else if (executionReport.isDone !== true) {
                messages = [t("ExecutionReport.statusMessages.running", { label })];
                notificationState = "neutral";
            } else {
                messages = [t("ExecutionReport.statusMessages.done", { label })];
                notificationState = "success";
            }
        }

        return (
            <div className="silk-report-warning">
                {messages.map((warning, idx) => (
                    <div key={idx}>
                        <Notification
                            neutral={notificationState === "neutral"}
                            success={notificationState === "success"}
                            warning={notificationState === "warning"}
                            danger={notificationState === "danger"}
                        >
                            {warning}
                        </Notification>
                        <Spacing size="tiny" />
                    </div>
                ))}
            </div>
        );
    };

    const renderTransformReport = () => {
        return (
            <Grid condensed>
                <GridRow>
                    <GridColumn medium>
                        <MappingsTree
                            currentRuleId={currentRuleId ?? "root"}
                            ruleTree={executionReport?.task.data.parameters.mappingRule}
                            showValueMappings={true}
                            handleRuleNavigation={onRuleNavigation}
                            ruleValidation={generateIcons()}
                            trackRuleInUrl={trackRuleInUrl}
                        />
                    </GridColumn>
                    <GridColumn>{renderRuleReport()}</GridColumn>
                </GridRow>
            </Grid>
        );
    };

    const renderEntityPreview = (id: string | undefined) => {
        const outputEntitiesSample = executionReport?.outputEntitiesSample;
        if (!outputEntitiesSample) {
            return null;
        }
        const dataPreviewPlugin = pluginRegistry.pluginReactComponent<DataPreviewProps>(SUPPORTED_PLUGINS.DATA_PREVIEW);
        let shownSamples: OutputEntitiesSample[] = outputEntitiesSample;
        if (id) {
            const specificSample = outputEntitiesSample.find((s) => s.id === id);
            if (specificSample) {
                shownSamples = [specificSample];
            } else {
                shownSamples = [];
            }
        }
        const containsEntities = !!shownSamples.find((es) => es.entities.length > 0);
        if (dataPreviewPlugin && containsEntities) {
            const types: string[] = [];
            const typeValues = new Map();
            shownSamples.forEach((entitiesSample) => {
                const { entities, schema } = entitiesSample;
                let type = schema?.typePath;
                if (!type) {
                    if (schema?.typeUri) {
                        type = schema?.typeUri;
                    } else if (entitiesSample.id) {
                        type = entitiesSample.id;
                    }
                }
                types.push(type);
                typeValues.set(type, {
                    attributes: schema
                        ? schema.properties
                        : entities[0].values.map((_v, idx) => `Attribute ${idx + 1}`),
                    values: entities.map((e) => e.values),
                });
            });
            return (
                <dataPreviewPlugin.Component
                    data-test-id={"execution-report-sample-entities"}
                    title={t("ExecutionReport.samplePreview.title")}
                    preview={{
                        types,
                        typeValues,
                    }}
                />
            );
        } else {
            return null;
        }
    };

    const generateIcons = () => {
        let ruleIcons = Object.create(null);
        for (let [ruleId, ruleResults] of Object.entries(executionReport?.ruleResults ?? {})) {
            if (ruleResults.errorCount === 0) {
                ruleIcons[ruleId] = "ok";
            } else {
                ruleIcons[ruleId] = "warning";
            }
        }
        return ruleIcons;
    };

    const renderRuleReport = () => {
        const ruleResults = currentRuleId ? executionReport?.ruleResults?.[currentRuleId] : undefined;
        let title;
        if (ruleResults === undefined) {
            title = t("ExecutionReport.transform.messages.selectMapping");
        } else if (ruleResults.errorCount === 0) {
            title = t("ExecutionReport.transform.messages.noIssues");
        } else {
            title = t("ExecutionReport.transform.messages.validationIssues", { errors: ruleResults.errorCount });
        }
        return (
            <Section className="ecc-silk-mapping__treenav">
                <Notification
                    neutral={ruleResults === undefined}
                    success={ruleResults?.errorCount === 0}
                    warning={(ruleResults?.errorCount ?? 0) > 0}
                >
                    {title}
                </Notification>
                {ruleResults !== undefined && ruleResults.errorCount > 0 && renderRuleErrors(ruleResults)}
                <Spacing size={"small"} />
                {renderEntityPreview(currentRuleId ?? "root")}
            </Section>
        );
    };

    const renderRuleErrors = (ruleResults) => {
        const actionFieldNeeded = !!ruleResults.sampleErrors[0]?.stacktrace;
        const columnWidths = actionFieldNeeded ? ["30%", "30%", "35%", "5%"] : ["30%", "30%", "40%"];
        return (
            <>
                <Spacing size="small" />
                <Table className="di-execution-report-table" useZebraStyles columnWidths={columnWidths}>
                    <TableHead>
                        <TableRow>
                            <TableHeader>{t("ExecutionReport.errorTable.entity")}</TableHeader>
                            <TableHeader>{t("ExecutionReport.errorTable.values")}</TableHeader>
                            <TableHeader>{t("ExecutionReport.errorTable.issue")}</TableHeader>
                            {actionFieldNeeded ? <TableHeader></TableHeader> : null}
                        </TableRow>
                    </TableHead>
                    <TableBody>{ruleResults.sampleErrors.map(renderRuleError)}</TableBody>
                </Table>
            </>
        );
    };

    const renderRuleError = (ruleError, idx) => {
        const hasStackTrace = !!ruleError.stacktrace;
        return (
            <TableRow key={idx}>
                <TableCell>
                    <HtmlContentBlock linebreakForced className="silk-report-errors-value">
                        {ruleError.entity}
                    </HtmlContentBlock>
                </TableCell>
                <TableCell>
                    <HtmlContentBlock linebreakForced className="silk-report-errors-value">
                        {ruleError.values.flat().join(", ")}
                    </HtmlContentBlock>
                </TableCell>
                <TableCell>
                    <HtmlContentBlock linebreakForced className="silk-report-errors-value">
                        {ruleError.error}
                    </HtmlContentBlock>
                </TableCell>
                {hasStackTrace ? (
                    <TableCell>
                        <SampleError sampleError={ruleError} />
                    </TableCell>
                ) : null}
            </TableRow>
        );
    };

    const isTransformReport = () => {
        return executionReport && "ruleResults" in executionReport;
    };

    return (
        <div data-test-id={"execution-report"}>
            {renderSummary()}
            {isTransformReport() && renderTransformReport()}
            {!isTransformReport() && renderEntityPreview(undefined)}
        </div>
    );
};

export default ExecutionReport;