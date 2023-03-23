import React from "react";
import PropTypes from "prop-types";
import {
    Card,
    CardContent,
    CardTitle,
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableRow,
} from "gui-elements-deprecated";
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
                <tr key="startedAt">
                    <td className="silk-report-table-bold">Started at</td>
                    <td>{this.props.executionMetaData.startedAt}</td>
                </tr>,
                <tr key="startedByUser">
                    <td className="silk-report-table-bold">Started by</td>
                    <td>
                        {this.props.executionMetaData.startedByUser == null
                            ? "Unknown"
                            : this.props.executionMetaData.startedByUser}
                    </td>
                </tr>,
                <tr key="finishedAt">
                    <td className="silk-report-table-bold">Finished at</td>
                    <td>{this.props.executionMetaData.finishedAt}</td>
                </tr>,
                <tr key="finishStatus">
                    <td className="silk-report-table-bold">Finish status</td>
                    <td>{this.props.executionMetaData.finishStatus.message}</td>
                </tr>,
            ]);
            if (this.props.executionMetaData.cancelledAt != null) {
                executionMetaData.push(
                    <tr key="cancelledAt">
                        <td className="silk-report-table-bold">Cancelled at</td>
                        <td>{this.props.executionMetaData.cancelledAt}</td>
                    </tr>
                );
            }
            if (this.props.executionMetaData.cancelledBy != null) {
                executionMetaData.push(
                    <tr key="cancelledBy">
                        <td className="silk-report-table-bold">Cancelled by</td>
                        <td>{this.props.executionMetaData.cancelledBy}</td>
                    </tr>
                );
            }
        }

        const summaryRows = this.props.executionReport.summary.map((v) => (
            <tr key={v.key}>
                <td className="silk-report-table-bold">{v.key}</td>
                <td>{v.value}</td>
            </tr>
        ));

        return (
            <Card className="silk-report-card">
                <CardTitle>{title}</CardTitle>
                <CardContent>
                    {this.renderWarning()}
                    <div>
                        <table className="silk-report-table">
                            <thead></thead>
                            <tbody>
                                {executionMetaData}
                                {summaryRows}
                            </tbody>
                        </table>
                    </div>
                </CardContent>
            </Card>
        );
    }

    renderWarning() {
        let messages = [];
        let alertClass = "mdl-alert--info";
        if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.cancelled) {
            messages = [`Task '${this.props.executionReport.label}' has been cancelled.`];
            alertClass = "mdl-alert--warning";
        } else if (
            (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.failed) ||
            this.props.executionReport.error != null
        ) {
            if (this.props.executionReport.error != null) {
                messages = [
                    `Task '${this.props.executionReport.label}' failed to execute. Details: ${this.props.executionReport.error}`,
                ];
            } else {
                messages = [`Task '${this.props.executionReport.label}' failed to execute.`];
            }
            alertClass = "mdl-alert--danger";
        } else if (this.props.executionReport.warnings.length > 0) {
            messages = this.props.executionReport.warnings;
            alertClass = "mdl-alert--info";
        } else if (this.props.executionReport.isDone !== true) {
            messages = [`Task '${this.props.executionReport.label}' has not finished execution yet.`];
            alertClass = "mdl-alert--info";
        } else {
            messages = [`Task '${this.props.executionReport.label}' has been executed without any issues.`];
            alertClass = "mdl-alert--success";
        }

        return (
            <div className="silk-report-warning">
                {messages.map((warning) => (
                    <div className={alertClass + " mdl-alert mdl-alert--border mdl-alert--spacing"}>
                        <div className="mdl-alert__content">
                            <p>{warning}</p>
                        </div>
                    </div>
                ))}
            </div>
        );
    }

    renderTransformReport() {
        return (
            <div className="mdl-grid mdl-grid--no-spacing">
                <div className="mdl-cell mdl-cell--3-col">
                    <MappingsTree
                        currentRuleId={this.state.currentRuleId ?? "root"}
                        ruleTree={this.props.executionReport.task.data.parameters.mappingRule}
                        showValueMappings={true}
                        handleRuleNavigation={this.onRuleNavigation}
                        ruleValidation={this.generateIcons()}
                        trackRuleInUrl={this.props.trackRuleInUrl}
                    />
                </div>
                <div className="mdl-cell mdl-cell--9-col">{this.renderRuleReport()}</div>
            </div>
        );
    }

    generateIcons() {
        let ruleIcons = {};
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
            title = "This mapping rule generated  " + ruleResults.errorCount + " validation issues during execution.";
        }
        return (
            <div className="ecc-silk-mapping__treenav">
                <Card className="mdl-card mdl-shadow--2dp mdl-card--stretch">
                    <div className="mdl-card__supporting-text">{title}</div>
                    {ruleResults !== undefined && ruleResults.errorCount > 0 && this.renderRuleErrors(ruleResults)}
                </Card>
            </div>
        );
    }

    renderRuleErrors(ruleResults) {
        return (
            <Table className="di-execution-report-table" style={{ width: "100%" }}>
                <TableHead>
                    <TableRow>
                        <TableCell isHead={true}>Entity</TableCell>
                        <TableCell isHead={true}>Values</TableCell>
                        <TableCell isHead={true}>Issue</TableCell>
                    </TableRow>
                </TableHead>
                <TableBody>{ruleResults.sampleErrors.map(this.renderRuleError)}</TableBody>
            </Table>
        );
    }

    renderRuleError(ruleError) {
        return (
            <TableRow key={ruleError.entity}>
                <TableCell>
                    <div className="silk-report-errors-value">{ruleError.entity}</div>
                </TableCell>
                <TableCell>
                    <div className="silk-report-errors-value">{ruleError.values.flat().join(", ")}</div>
                </TableCell>
                <TableCell>
                    <div className="silk-report-errors-value">{ruleError.error}</div>
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
