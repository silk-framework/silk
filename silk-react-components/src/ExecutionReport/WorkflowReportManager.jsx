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
        this.setState({
            selectedReport: this.props.report
        });

        this.props.diStore.listExecutionReports(
            this.props.baseUrl,
            this.props.project,
            this.props.task)
            .then((reports) => {
                this.setState({
                    availableReports: reports
                });
            })
            .catch((error) => {
                console.log("Loading execution reports failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
            });
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevState.selectedReport !== this.state.selectedReport) {
            const href = window.location.href;
            try {
                const uriTemplate = new URI(href);
                const updatedUrl = WorkflowReportManager.updateMappingEditorUrl(uriTemplate, this.state.selectedReport);
                history.pushState(null, '', updatedUrl);
            } catch (e) {
                console.debug(`ReportManager: ${href} is not an URI, cannot update the window state`);
            }
        }
    }

    render() {
        return <div>
            { this.renderReportChooser() }
            { this.renderSelectedReport() }
        </div>
    }

    renderReportChooser() {
        return <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
            <div className="mdl-card__actions">
                <select name="reports" id="reports" value={this.state.selectedReport} onChange={e => this.setState({selectedReport: e.target.value})}>
                    <option key="" value="">Select report</option>
                    { this.state.availableReports.map(e => this.renderReportItem(e)) }
                </select>
            </div>
        </div>
    }

    renderReportItem(report) {
        return <option key={report.time} value={report.time}>{new Date(report.time).toString()}</option>
    }

    renderSelectedReport() {
        if(this.state.selectedReport == null || this.state.selectedReport === "") {
            return <div>No report selected</div>
        } else {
            return <WorkflowExecutionReport baseUrl={this.props.baseUrl}
                                            project={this.props.project}
                                            task={this.props.task}
                                            time={this.state.selectedReport} />
        }
    }

    static updateMappingEditorUrl = (currentUrl, newReport) => {
        const segments = currentUrl.segment();
        const reportIdx = segments.findIndex((segment) => segment === "report");
        currentUrl.segment(reportIdx + 3, newReport);
        return currentUrl.toString();
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
