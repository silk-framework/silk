// FIXME: should be transcoded to a tsx file

import React from "react";
import PropTypes from "prop-types";
import _ from "lodash";
import {
    ApplicationViewability,
    Card,
    Grid,
    GridRow,
    GridColumn,
    HtmlContentBlock,
    Icon,
    Link,
    OverviewItemList,
    OverviewItem,
    Depiction,
    OverviewItemDescription,
    OverviewItemLine,
    OverflowText,
    Section,
    SectionHeader,
    Spacing,
    TitleSubsection,
} from "@eccenca/gui-elements";
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";

/**
 * Displays a workflow execution report.
 */
export default class WorkflowExecutionReport extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = "WorkflowExecutionReport";
        this.state = {
            selectedIndex: -1, // the index of the selected task report or -1 for the workflow itself
        };
        this.reportNavId = _.uniqueId("silk-workflow-report-nav-");
        this.reportTargetId = _.uniqueId("silk-workflow-report-target-");
    }

    render() {
        // Warnings of the workflow itself, e.g. undefined execution order of 'Clear dataset' nodes.
        let executionWarnings = this.props.executionReport.warnings || [];
        if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.cancelled) {
            executionWarnings = ["Executed cancelled", ...executionWarnings];
        } else if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.failed) {
            executionWarnings = ["Executed failed", ...executionWarnings];
        }

        return (
            <Grid useAbsoluteSpace verticalStretchable>
                <GridRow verticalStretched>
                    <GridColumn small style={{ overflow: "auto", paddingLeft: 0 }}>
                        <nav id={this.reportNavId} aria-label="Execution report outline">
                            <Section className="silk-report-sidebar-overview">
                                <SectionHeader>
                                    <TitleSubsection>Workflow</TitleSubsection>
                                </SectionHeader>
                                <div>
                                    <OverviewItemList hasSpacing>
                                        {this.renderTaskItem(this.props.executionReport, -1, executionWarnings)}
                                    </OverviewItemList>
                                </div>
                            </Section>
                            <Spacing />
                            <Section className="silk-report-sidebar-tasks">
                                <SectionHeader className="silk-report-sidebar-tasks-title">
                                    <TitleSubsection>Tasks</TitleSubsection>
                                </SectionHeader>
                                <div className="silk-report-sidebar-tasks-content">
                                    <OverviewItemList hasSpacing>
                                        {this.props.executionReport.taskReports.map((report, index) =>
                                            this.renderTaskItem(report, index, report.warnings),
                                        )}
                                    </OverviewItemList>
                                </div>
                            </Section>
                        </nav>
                    </GridColumn>
                    <GridColumn style={{ overflow: "auto" }}>
                        {this.renderReportTarget(this.renderReport())}
                    </GridColumn>
                </GridRow>
            </Grid>
        );
    }

    selectReport(index) {
        // Move focus to the report after it was rendered, so keyboard and screen reader users land on the new content
        this.setState({ selectedIndex: index }, () => {
            const reportTarget = document.getElementById(this.reportTargetId);
            if (reportTarget) {
                reportTarget.focus();
            }
        });
    }

    renderTaskItem(report, index, warnings) {
        let classNames = "silk-report-list-item";
        if (index === this.state.selectedIndex) {
            classNames += " silk-report-list-item-icon-selected";
        }

        return (
            <Card
                isOnlyLayout
                elevated={index === this.state.selectedIndex}
                key={"report-" + index}
                className={classNames}
                onClick={() => this.selectReport(index)}
                onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === " ") {
                        event.preventDefault();
                    }
                    if (event.key === "Enter") {
                        event.currentTarget.click();
                    }
                }}
                onKeyUp={(event) => {
                    if (event.key === " ") {
                        event.currentTarget.click();
                    }
                }}
                aria-current={index === this.state.selectedIndex ? true : undefined}
                aria-controls={this.reportTargetId}
                tabIndex={0}
                role={"button"}
            >
                <OverviewItem densityHigh hasSpacing>
                    {this.renderTaskIcon(warnings, report.error)}
                    <OverviewItemDescription>
                        <OverviewItemLine small>
                            <OverflowText>
                                {report.label} {report.operation != null ? "(" + report.operation + ")" : ""}
                            </OverflowText>
                        </OverviewItemLine>
                        <OverviewItemLine small>{this.renderTaskDescription(warnings, report.error)}</OverviewItemLine>
                    </OverviewItemDescription>
                </OverviewItem>
            </Card>
        );
    }

    renderTaskDescription(warnings, error) {
        if (error) {
            return <OverflowText>Execution failed</OverflowText>;
        } else if (warnings != null && warnings.length > 0) {
            return <OverflowText>{warnings.length} warning(s)</OverflowText>;
        } else {
            return <OverflowText>No issues</OverflowText>;
        }
    }

    renderTaskIcon(warnings, error) {
        if (error) {
            return <Depiction image={<Icon name="state-danger" intent={"danger"} />} />;
        } else if (warnings != null && warnings.length > 0) {
            return <Depiction image={<Icon name="state-warning" intent={"warning"} />} />;
        } else {
            return <Depiction image={<Icon name="state-success" intent={"success"} />} />;
        }
    }

    renderReport() {
        if (this.state.selectedIndex >= 0) {
            const taskReport = this.props.executionReport.taskReports[this.state.selectedIndex];
            if ("taskReports" in taskReport) {
                // This is a nested workflow execution report
                return <WorkflowExecutionReport project={this.props.project} executionReport={taskReport} />;
            } else {
                // Render the report of the selected task
                return <ExecutionReport executionReport={taskReport} />;
            }
        } else {
            // Render the report of the workflow itself
            return (
                <ExecutionReport
                    project={this.props.project}
                    nodeId={this.props.executionReport.task.id}
                    executionReport={this.props.executionReport}
                    executionMetaData={this.props.executionMetaData}
                />
            );
        }
    }

    renderReportTarget(reportElement) {
        return (
            <article id={this.reportTargetId} tabIndex={-1} aria-label={"Workflow execution feedback"}>
                <ApplicationViewability hide={"screen"}>
                    <HtmlContentBlock>
                        <p>
                            <Link href={`#${this.reportNavId}`}>Back to report outline</Link>
                        </p>
                    </HtmlContentBlock>
                </ApplicationViewability>
                {reportElement}
            </article>
        );
    }
}

WorkflowExecutionReport.propTypes = {
    project: PropTypes.string.isRequired, // project ID
    executionMetaData: PropTypes.object,
    executionReport: PropTypes.object.isRequired,
    diStore: PropTypes.shape({
        retrieveExecutionReport: PropTypes.func,
    }), // DI store object that provides the business layer API to DI related services
};

WorkflowExecutionReport.defaultProps = {
    diStore: silkStore,
};
