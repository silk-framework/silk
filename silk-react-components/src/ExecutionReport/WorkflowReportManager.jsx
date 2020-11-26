import React from 'react';
import PropTypes from 'prop-types';
import {Card, CardContent, CardTitle, Icon} from '@eccenca/gui-elements';
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";
import WorkflowExecutionReport from "./WorkflowExecutionReport";
import _ from "lodash";
import {URI} from "ecc-utils";

/**
 * Let's the user view execution reports.
 */
export default class WorkflowReportManager extends React.Component {

    constructor(props) {
        super(props);
        this.displayName = 'WorkflowReportManager';
        this.state = {
            availableReports: [],
            selectedReport: ""
        };
    }

    componentDidMount() {
        this.props.diStore.listExecutionReports(
            this.props.baseUrl,
            this.props.project,
            this.props.task)
            .then((reports) => {
                // Determine which report should be selected initially
                let selectedReport;
                if(reports.length === 0) {
                    // No reports are available
                    selectedReport = "";
                } else if(this.props.report != null && this.props.report !== "") {
                    // Select the report provided in the props
                    selectedReport = this.props.report;
                } else {
                    // Select last (i.e., most recent) report
                    selectedReport = reports[reports.length-1].time;
                }
                // Set initial state
                this.setState({
                    availableReports: reports,
                    selectedReport: selectedReport
                });
            })
            .catch((error) => {
                console.log("Loading execution reports failed! " + error);
            });
    }

    render() {
        if(this.state.availableReports.length > 0) {
            return <div>
                { this.renderReportChooser() }
                { this.renderSelectedReport() }
            </div>
        } else {
            return this.renderNoReport();
        }
    }

    renderNoReport() {
        return <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
            <div className="mdl-card__actions">
                <div className="mdl-alert mdl-alert--info mdl-alert--border mdl-alert--spacing">
                    <div className="mdl-alert__content">
                        <p>There are no execution reports available for this workflow. Please execute the workflow in order to create an execution report.</p>
                    </div>
                </div>
            </div>
        </div>
    }

    renderReportChooser() {
        return <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
            <div className="mdl-card__actions">
                <select name="reports" id="reports" value={this.state.selectedReport} onChange={e => this.updateSelectedReport(e.target.value)}>
                    { this.state.availableReports.map(e => this.renderReportItem(e)) }
                </select>
            </div>
        </div>
    }

    renderReportItem(report) {
        return <option key={report.time} value={report.time}>{new Date(report.time).toString()}</option>
    }

    renderSelectedReport() {
        return <WorkflowExecutionReport baseUrl={this.props.baseUrl}
                                        project={this.props.project}
                                        task={this.props.task}
                                        time={this.state.selectedReport} />
    }

    updateSelectedReport(newReport) {
        this.setState({selectedReport: newReport});
        this.updateUrl();
    }

    // Updates the window URL based on the selected report
    updateUrl() {
        const href = window.location.href;
        try {
            const uriTemplate = new URI(href);
            const segments = uriTemplate.segment();
            const reportIdx = segments.findIndex((segment) => segment === "report");
            uriTemplate.segment(reportIdx + 3, this.state.selectedReport);
            history.pushState(null, '', uriTemplate.toString());
        } catch (e) {
            console.debug(`ReportManager: ${href} is not an URI, cannot update the window state`);
        }
    }
}

WorkflowReportManager.propTypes = {
    baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
    project: PropTypes.string.isRequired, // project ID
    task: PropTypes.string.isRequired, // task ID
    report: PropTypes.string, // optional report time
    diStore: PropTypes.shape({
        listExecutionReports: PropTypes.func,
    }) // DI store object that provides the business layer API to DI related services
};

WorkflowReportManager.defaultProps = {
    diStore: silkStore
};
