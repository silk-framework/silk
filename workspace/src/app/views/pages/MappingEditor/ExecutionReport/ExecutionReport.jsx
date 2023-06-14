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

/**
 * Displays a task execution report.
 */
export default class ExecutionReport extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = "ExecutionReport";
        this.state = {
            currentRuleId: null,
        };
        this.onRuleNavigation = this.onRuleNavigation.bind(this);
    }

    componentDidUpdate(prevProps) {
        const ruleResults = this.props.executionReport?.ruleResults;
        const ruleResultsChanged = prevProps.executionReport?.ruleResults !== ruleResults;
        if (ruleResults && ruleResultsChanged) {
            const initialRuleId = new URLSearchParams(window.location.search).get("ruleId");
            if (initialRuleId && ruleResults[initialRuleId]) {
                this.onRuleNavigation({ newRuleId: initialRuleId });
            }
        }
    }

    onRuleNavigation({ newRuleId }) {
        this.setState({ currentRuleId: newRuleId });
    }

    renderSummary() {
        let title;
        if (this.props.executionReport.entityCount != null && this.props.executionReport.operationDesc != null) {
            title =
                "Execution: " + this.props.executionReport.entityCount + " " + this.props.executionReport.operationDesc;
        } else {
            title = "Execution Report";
        }

        let executionMetaData = [];
        if (this.props.executionMetaData != null) {
            executionMetaData = executionMetaData.concat([
                <PropertyValuePair hasDivider key="startedAt">
                    <PropertyName className="silk-report-table-bold">Started at</PropertyName>
                    <PropertyValue>{this.props.executionMetaData.startedAt}</PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="startedByUser">
                    <PropertyName className="silk-report-table-bold">Started by</PropertyName>
                    <PropertyValue>
                        {this.props.executionMetaData.startedByUser == null
                            ? "Unknown"
                            : this.props.executionMetaData.startedByUser}
                    </PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="finishedAt">
                    <PropertyName className="silk-report-table-bold">Finished at</PropertyName>
                    <PropertyValue>{this.props.executionMetaData.finishedAt}</PropertyValue>
                </PropertyValuePair>,
                <PropertyValuePair hasDivider key="finishStatus">
                    <PropertyName className="silk-report-table-bold">Finish status</PropertyName>
                    <PropertyValue>{this.props.executionMetaData.finishStatus.message}</PropertyValue>
                </PropertyValuePair>,
            ]);
            if (this.props.executionMetaData.cancelledAt != null) {
                executionMetaData.push(
                    <PropertyValuePair hasDivider key="cancelledAt">
                        <PropertyName className="silk-report-table-bold">Cancelled at</PropertyName>
                        <PropertyValue>{this.props.executionMetaData.cancelledAt}</PropertyValue>
                    </PropertyValuePair>
                );
            }
            if (this.props.executionMetaData.cancelledBy != null) {
                executionMetaData.push(
                    <PropertyValuePair hasDivider key="cancelledBy">
                        <PropertyName className="silk-report-table-bold">Cancelled by</PropertyName>
                        <PropertyValue>{this.props.executionMetaData.cancelledBy}</PropertyValue>
                    </PropertyValuePair>
                );
            }
        }

        const summaryRows = this.props.executionReport.summary.map((v) => (
            <PropertyValuePair hasDivider key={v.key}>
                <PropertyName className="silk-report-table-bold">{v.key}</PropertyName>
                <PropertyValue>{v.value}</PropertyValue>
            </PropertyValuePair>
        ));

        return (
            <Section className="silk-report-card">
                {this.renderWarning()}
                <Notification info>
                    <p>{title}</p>
                    <PropertyValueList className="silk-report-table">
                        {executionMetaData}
                        {summaryRows}
                    </PropertyValueList>
                </Notification>
                <Spacing size="tiny" />
            </Section>
        );
    }

    renderWarning() {
        let messages = [];
        let notificationState = "info";
        if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.cancelled) {
            messages = [`Task '${this.props.executionReport.label}' has been cancelled.`];
            notificationState = "warning";
        } else if (this.props.executionReport.error != null) {
            messages = [
                `Task '${this.props.executionReport.label}' failed to execute. Details: ${this.props.executionReport.error}`,
            ];
            notificationState = "danger";
        } else if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.failed) {
            messages = [`Task '${this.props.executionReport.label}' failed to execute.`];
            notificationState = "danger";
        } else if (this.props.executionReport.warnings.length > 0) {
            messages = this.props.executionReport.warnings;
            notificationState = "neutral";
        } else if (this.props.executionReport.isDone !== true) {
            messages = [`Task '${this.props.executionReport.label}' has not finished execution yet.`];
            notificationState = "neutral";
        } else {
            messages = [`Task '${this.props.executionReport.label}' has been executed without any issues.`];
            notificationState = "success";
        }

        return (
            <div className="silk-report-warning">
                {messages.map((warning, idx) => (
                    <div key={idx}>
                        <Notification
                            neutral={notificationState === "neutral"}
                            info={notificationState === "info"}
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
    }

    renderTransformReport() {
        return (
            <Grid condensed>
                <GridRow>
                    <GridColumn medium>
                        <MappingsTree
                            currentRuleId={this.state.currentRuleId ?? "root"}
                            ruleTree={this.props.executionReport.task.data.parameters.mappingRule}
                            showValueMappings={true}
                            handleRuleNavigation={this.onRuleNavigation}
                            ruleValidation={this.generateIcons()}
                            trackRuleInUrl={this.props.trackRuleInUrl}
                        />
                    </GridColumn>
                    <GridColumn>{this.renderRuleReport()}</GridColumn>
                </GridRow>
            </Grid>
        );
    }

    generateIcons() {
        let ruleIcons = Object.create(null);
        for (let [ruleId, ruleResults] of Object.entries(this.props.executionReport.ruleResults)) {
            if (ruleResults.errorCount === 0) {
                ruleIcons[ruleId] = "ok";
            } else {
                ruleIcons[ruleId] = "warning";
            }
        }
        return ruleIcons;
    }

    renderRuleReport() {
        const ruleResults = this.props.executionReport.ruleResults[this.state.currentRuleId];
        let title;
        if (ruleResults === undefined) {
            title = "Select a mapping for detailed results.";
        } else if (ruleResults.errorCount === 0) {
            title = "This mapping rule executed successfully without any issues.";
        } else {
            title =
                "This mapping rule generated  " +
                ruleResults.errorCount +
                " validation issues during execution. Examples are shown below.";
        }
        return (
            <Section className="ecc-silk-mapping__treenav">
                <Notification
                    neutral={ruleResults === undefined}
                    success={ruleResults?.errorCount === 0}
                    warning={ruleResults?.errorCount > 0}
                >
                    {title}
                </Notification>
                {ruleResults !== undefined && ruleResults.errorCount > 0 && this.renderRuleErrors(ruleResults)}
            </Section>
        );
    }

    renderRuleErrors(ruleResults) {
        return (
            <>
                <Spacing size="small" />
                <Table className="di-execution-report-table" useZebraStyles columnWidths={["30%", "30%", "40%"]}>
                    <TableHead>
                        <TableRow>
                            <TableHeader>Entity</TableHeader>
                            <TableHeader>Values</TableHeader>
                            <TableHeader>Issue</TableHeader>
                        </TableRow>
                    </TableHead>
                    <TableBody>{ruleResults.sampleErrors.map(this.renderRuleError)}</TableBody>
                </Table>
            </>
        );
    }

    renderRuleError(ruleError, idx) {
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
            </TableRow>
        );
    }

    isTransformReport() {
        return "ruleResults" in this.props.executionReport;
    }

    render() {
        return (
            <div data-test-id={"execution-report"}>
                {this.renderSummary()}
                {this.isTransformReport() && this.renderTransformReport()}
            </div>
        );
    }
}

ExecutionReport.propTypes = {
    project: PropTypes.string.isRequired, // project ID
    nodeId: PropTypes.string.isRequired, // workflow node ID
    executionReport: PropTypes.object, // The execution report to render
    executionMetaData: PropTypes.object, // Optional execution meta data that includes start time, user, etc.
    trackRuleInUrl: PropTypes.bool,
};

ExecutionReport.defaultProps = {
    trackRuleInUrl: true,
};
