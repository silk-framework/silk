import { URI } from "ecc-utils";
import PropTypes from "prop-types";
import React from "react";

import silkStore from "../api/silkStore";
import { withHistoryHOC } from "../HierarchicalMapping/utils/withHistoryHOC";
import WorkflowExecutionReport from "./WorkflowExecutionReport";

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
                    {this.renderSelectedReport()}
                </div>
            );
        } else {
            return this.renderNoReport();
        }
    }

    renderNoReport() {
        return (
            <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
                <div className="mdl-card__actions">
                    <div className="mdl-alert mdl-alert--info mdl-alert--border mdl-alert--spacing">
                        <div className="mdl-alert__content">
                            <p>
                                There are no execution reports available for this workflow. Please execute the workflow
                                in order to create an execution report.
                            </p>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    renderReportChooser() {
        return (
            <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
                <div className="mdl-card__actions">
                    <select
                        name="reports"
                        id="reports"
                        value={this.state.selectedReport}
                        onChange={(e) => this.updateSelectedReport(e.target.value)}
                    >
                        {this.state.availableReports.map((e) => this.renderReportItem(e))}
                    </select>
                </div>
            </div>
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
