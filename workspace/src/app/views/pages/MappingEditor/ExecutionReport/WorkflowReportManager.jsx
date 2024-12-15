import // FIXME: should be transcoded to a tsx file

React from "react";
import PropTypes from "prop-types";
import { URI } from "ecc-utils";
import { IconButton, Notification, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { withHistoryHOC } from "../HierarchicalMapping/utils/withHistoryHOC";
import silkStore from "../api/silkStore";
import WorkflowExecutionReport from "./WorkflowExecutionReport";
import { CONTEXT_PATH } from "../../../../constants/path";

/**
 * Let's the user view execution reports.
 */
class WorkflowReportManager extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = "WorkflowReportManager";
        this.state = {
            availableReports: [],
            selectedReport: "", // Id of the current report
            executionMetaData: null, //Meta data of the current report
            executionReport: {
                summary: [],
                warnings: [],
                task: {
                    id: "workflow",
                },
                taskReports: [],
            },
        };
    }

    componentDidMount() {
        this.props.diStore
            .listExecutionReports(this.props.project, this.props.task)
            .then((reports) => {
                // Determine which report should be selected initially
                let selectedReport;
                if (reports.length === 0) {
                    // No reports are available
                    selectedReport = "";
                } else if (this.props.report != null && this.props.report !== "") {
                    // Select the report provided in the props
                    selectedReport = this.props.report;
                } else {
                    // Select the first (i.e., most recent) report
                    selectedReport = reports[0].time;
                }
                // Set initial state
                this.setState({
                    availableReports: reports,
                });
                // Load initial report
                this.updateSelectedReport(selectedReport);
            })
            .catch((error) => {
                console.log("Loading execution reports failed! " + error);
            });
    }

    render() {
        if (this.state.availableReports.length > 0) {
            return (
                <div>
                    {this.renderReportChooser()}
                    <Spacing />
                    {this.renderSelectedReport()}
                </div>
            );
        } else {
            return <div>{this.renderNoReport()}</div>;
        }
    }

    renderNoReport() {
        return (
            <Notification>
                There are no execution reports available for this workflow. Please execute the workflow in order to
                create an execution report.
            </Notification>
        );
    }

    renderReportChooser() {
        // FIXME: should be a select using the select styles but this is currently not provided via the GUI elements
        return (
            <Toolbar>
                <ToolbarSection canGrow>
                    <select
                        name="reports"
                        id="reports"
                        value={this.state.selectedReport}
                        onChange={(e) => this.updateSelectedReport(e.target.value)}
                        style={{ width: "100%", padding: "7px" }}
                    >
                        {this.state.availableReports.map((e) => this.renderReportItem(e))}
                    </select>
                </ToolbarSection>
                <ToolbarSection>
                    <Spacing vertical size="tiny" />
                    <IconButton
                        name="item-download"
                        download={`report_${new Date(this.state.selectedReport).getTime()}`}
                        href={`${CONTEXT_PATH}/api/workspace/reports/report?projectId=${this.props.project}&taskId=${this.props.task}&time=${this.state.selectedReport}`}
                    />
                </ToolbarSection>
            </Toolbar>
        );
    }

    renderReportItem(report) {
        return (
            <option key={report.time} value={report.time}>
                {new Date(report.time).toString()}
            </option>
        );
    }

    renderSelectedReport() {
        return (
            <WorkflowExecutionReport
                project={this.props.project}
                executionMetaData={this.state.executionMetaData}
                executionReport={this.state.executionReport}
            />
        );
    }

    updateSelectedReport(newReport) {
        this.props.diStore
            .retrieveExecutionReport(this.props.project, this.props.task, newReport)
            .then((report) => {
                this.setState({
                    selectedReport: newReport,
                    executionReport: report.value,
                    executionMetaData: report.metaData,
                });
                this.updateUrl();
            })
            .catch((error) => {
                console.log("Loading execution report failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
            });
    }

    // Updates the window URL based on the selected report
    updateUrl() {
        const href = window.location.href;
        try {
            const uriTemplate = new URI(href);
            const segments = uriTemplate.segment();
            const reportIdx = segments.findIndex((segment) => segment === "report");
            uriTemplate.segment(reportIdx + 3, this.state.selectedReport);
            this.props.history.pushState(null, "", uriTemplate.toString());
        } catch (e) {
            console.debug(`ReportManager: ${href} is not an URI, cannot update the window state`);
        }
    }
}

WorkflowReportManager.propTypes = {
    project: PropTypes.string.isRequired, // project ID
    task: PropTypes.string.isRequired, // task ID
    report: PropTypes.string, // optional report time
    diStore: PropTypes.shape({
        listExecutionReports: PropTypes.func,
    }), // DI store object that provides the business layer API to DI related services
};

WorkflowReportManager.defaultProps = {
    diStore: silkStore,
};

export default withHistoryHOC(WorkflowReportManager);
