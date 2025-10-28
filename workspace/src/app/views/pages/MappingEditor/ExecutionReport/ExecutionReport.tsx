import React from "react";
import {
    Grid,
    GridColumn,
    GridRow,
    HtmlContentBlock,
    Notification,
    NotificationProps,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    Section,
    Spacing,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
    TestIcon,
} from "@eccenca/gui-elements";
import MappingsTree, {RuleValidationIconMapType} from "../HierarchicalMapping/containers/MappingsTree";
import {SampleError} from "../../../shared/SampleError/SampleError";
import {pluginRegistry, SUPPORTED_PLUGINS} from "../../../plugins/PluginRegistry";
import {DataPreviewProps} from "../../../plugins/plugin.types";
import {ExecutionReportResponse, OutputEntitiesSample, TypeRuleData} from "./report-typings";
import {useTranslation} from "react-i18next";
import {MAPPING_RULE_TYPE_OBJECT} from "../HierarchicalMapping/utils/constants";
import {MAPPING_ROOT_RULE_ID} from "../HierarchicalMapping/HierarchicalMapping";
import { InProgressError, InProgressWarning } from "@carbon/icons-react";

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

    const onRuleNavigation = React.useCallback(({ newRuleId }: { newRuleId: string }) => {
        setCurrentRuleId(newRuleId);
    }, []);

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
                    </PropertyValuePair>,
                );
            }
            if (executionMetaData.cancelledBy != null) {
                executionMetaDataPairs.push(
                    <PropertyValuePair hasDivider key="cancelledBy">
                        <PropertyName className="silk-report-table-bold">Cancelled by</PropertyName>
                        <PropertyValue>{executionMetaData.cancelledBy}</PropertyValue>
                    </PropertyValuePair>,
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
                        <Notification intent={notificationState as NotificationProps["intent"]}>{warning}</Notification>
                        <Spacing size="tiny" />
                    </div>
                ))}
            </div>
        );
    };

    // Returns all type rules associated with the mappingRule
    const typeRules = (mappingRule: any): Map<string, TypeRuleData[]> => {
        const m = new Map<string, TypeRuleData[]>();
        const traverse = (rule: any): void => {
            const rules = rule.rules;
            if (rules.typeRules != null) {
                m.set(
                    rule.id,
                    rules.typeRules.map((r) => ({ id: r.id, typeRuleId: r.typeUri })),
                );
            }
            if (rules.propertyRules != null) {
                rules.propertyRules.filter(({ type }) => type === MAPPING_RULE_TYPE_OBJECT).forEach((r) => traverse(r));
            }
        };
        if (mappingRule) {
            traverse(mappingRule);
        }
        return m;
    };

    const renderTransformReport = () => {
        const ruleValidationIcons = generateIcons();
        const mappingRule = executionReport?.task.data.parameters.mappingRule;
        return (
            <Grid condensed>
                <GridRow>
                    <GridColumn medium>
                        <MappingsTree
                            currentRuleId={currentRuleId ?? MAPPING_ROOT_RULE_ID}
                            ruleTree={mappingRule}
                            showValueMappings={true}
                            handleRuleNavigation={onRuleNavigation}
                            ruleValidation={ruleValidationIcons}
                            trackRuleInUrl={trackRuleInUrl}
                        />
                    </GridColumn>
                    <GridColumn>{renderRuleReport(ruleValidationIcons)}</GridColumn>
                </GridRow>
            </Grid>
        );
    };

    const renderEntityPreview = (id: string | undefined, showURI: boolean) => {
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
                const attributes = schema
                    ? schema.properties
                    : entities[0].values.map((_v, idx) => `Attribute ${idx + 1}`);
                const values = entities.map((e) => {
                    if (showURI) {
                        return [...e.values, [e.uri]];
                    } else {
                        return e.values;
                    }
                });
                if (showURI) {
                    attributes.push("URI");
                }
                typeValues.set(type, {
                    attributes,
                    values,
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

    const generateIcons = (): RuleValidationIconMapType => {
        let ruleIcons: RuleValidationIconMapType = Object.create(null);
        for (let [ruleId, ruleResults] of Object.entries(executionReport?.ruleResults ?? {})) {
            if(!ruleResults.finishedAt || !ruleResults.startedAt) {
                // Either never started or did not finish successfully
                if(!ruleResults.startedAt) {
                    ruleIcons[ruleId] = <TestIcon className="ecc-silk-mapping__ruleitem-icon-yellow" tryout={InProgressWarning} />
                } else {
                    ruleIcons[ruleId] = <TestIcon className="ecc-silk-mapping__ruleitem-icon-yellow" tryout={InProgressError} />
                }
            } else if (ruleResults.errorCount === 0) {
                ruleIcons[ruleId] = "ok";
            } else {
                ruleIcons[ruleId] = "warning";
            }
        }
        return ruleIcons;
    };

    const renderRuleReport = (ruleValidation: RuleValidationIconMapType) => {
        const ruleId = currentRuleId ?? MAPPING_ROOT_RULE_ID;
        const mappingRule = executionReport?.task.data.parameters.mappingRule;
        const typeRulesPerContainerRule = typeRules(mappingRule);
        const ruleResults = executionReport?.ruleResults?.[ruleId];
        let title: string | undefined = undefined;
        let validationError: string | undefined = undefined
        let intent: "neutral" | "warning" | "success" = "neutral"
        let typeRulesWithIssues: TypeRuleData[] = [];
        const showURI = !!executionReport?.executionReportContext?.entityUriOutput;
        if (ruleResults) {
            if(!ruleResults.finishedAt || !ruleResults.startedAt) {
                // Either never started or did not finish successfully
                if(!ruleResults.startedAt) {
                    title = t("ExecutionReport.transform.messages.notExecuted");
                } else {
                    title = t("ExecutionReport.transform.messages.notFinished");
                }
                intent = "warning"
            } else if (ruleResults.errorCount === 0) {
                title = t("ExecutionReport.transform.messages.noIssues");
                intent = "success"
            }
            if(ruleResults.errorCount > 0) {
                const errorCount = `${ruleResults.errorCount}`;
                validationError = t("ExecutionReport.transform.messages.validationIssues", { errors: errorCount });
            }
        }
        // Check type rules
        const typeRulesOfRule = typeRulesPerContainerRule.get(ruleId) ?? [];
        typeRulesWithIssues = typeRulesOfRule.filter(
            (typeRuleId) => ruleValidation && ruleValidation[typeRuleId.id] === "warning",
        );
        return (
            <Section className="ecc-silk-mapping__treenav">
                {typeRulesWithIssues.length ? (
                    <Notification data-test-id={"type-rule-validation-issues"} intent="warning">
                        {t("ExecutionReport.transform.messages.InvalidTypeUris", {
                            typeUris: typeRulesWithIssues.map((r) => r.typeRuleId).join(", "),
                        })}
                    </Notification>
                ) : null}
                {title ? (
                    <Notification intent={intent}>
                        {title}
                    </Notification>
                ) : null}
                {title && validationError ?
                    <Spacing size={"tiny"} /> :
                    null
                }
                {validationError ?
                    <Notification intent={"warning"}>
                        {validationError}
                    </Notification> :
                    null
                }
                {ruleResults !== undefined && ruleResults.errorCount > 0 && renderRuleErrors(ruleResults)}
                <Spacing size={"small"} />
                {renderEntityPreview(ruleId, showURI)}
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
            {!isTransformReport() && renderEntityPreview(undefined, false)}
        </div>
    );
};

export default ExecutionReport;
