// FIXME: should be transcoded to a tsx file

import React from "react";
import PropTypes from "prop-types";
import {
    Card,
    Grid,
    GridRow,
    GridColumn,
    Icon,
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
    }

    render() {
        let executionWarnings = [];
        if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.cancelled) {
            executionWarnings = ["Executed cancelled"];
        } else if (this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.failed) {
            executionWarnings = ["Executed failed"];
        }

        return (
            <Grid useAbsoluteSpace verticalStretchable>
                <GridRow verticalStretched>
                    <GridColumn small style={{ overflow: "auto", paddingLeft: 0 }}>
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
                    </GridColumn>
                    <GridColumn style={{ overflow: "auto" }}>
                        {this.renderReport(this.props.executionReport.nodeId)}
                    </GridColumn>
                </GridRow>
            </Grid>
        );
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
                onClick={() => this.setState({ selectedIndex: index })}
            >
                <OverviewItem densityHigh hasSpacing>
                    {this.renderTaskIcon(warnings, report.error)}
                    <OverviewItemDescription>
                        <OverviewItemLine small>
                            <OverflowText>
                                {report.label} {report.operation != null ? "(" + report.operation + ")" : ""}
                            </OverflowText>
                        </OverviewItemLine>
                        <OverviewItemLine small>{this.renderTaskDescription(warnings)}</OverviewItemLine>
                    </OverviewItemDescription>
                </OverviewItem>
            </Card>
        );
    }

    renderTaskDescription(warnings) {
        if (warnings != null && warnings.length > 0) {
            return <OverflowText>{warnings.length} warnings</OverflowText>;
        } else {
            return <OverflowText>no issues</OverflowText>;
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

    renderReport(nodeId) {
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
