import React from 'react';
import PropTypes from 'prop-types';
import {Card, CardContent, CardTitle, Icon} from '@eccenca/gui-elements';
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";
import WorkflowExecutionReport from "./WorkflowExecutionReport";

/**
 * Let's the user view execution reports.
 */
export default class WorkflowReportManager extends React.Component {

    constructor(props) {
        super(props);
        this.displayName = 'WorkflowReportManager';
        this.state = {
            availableReports: [],
            selectedReportTime: null
        };
    }

    componentDidMount() {
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

    render() {
        return <div>
            { this.renderReportChooser() }
            { this.renderSelectedReport() }
        </div>
    }

    renderReportChooser() {
        return <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
            <div className="mdl-card__actions">
                <select name="reports" id="reports" onChange={e => this.setState({selectedReportTime: e.target.value})}>
                    { this.state.availableReports.map(e => this.renderReportItem(e)) }
                </select>
            </div>
        </div>
    }

    renderReportItem(report) {
        return <option value={report.time}>{report.time}</option>
    }

    renderSelectedReport() {
        if(this.state.selectedReportTime == null) {
            return <div>No report selected</div>
        } else {
            return <WorkflowExecutionReport baseUrl={this.props.baseUrl}
                                            project={this.props.project}
                                            task={this.props.task}
                                            time={this.state.selectedReportTime} />
        }
    }
}

WorkflowReportManager.propTypes = {
    baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
    project: PropTypes.string.isRequired, // project ID
    task: PropTypes.string.isRequired, // task ID
    diStore: PropTypes.shape({
        listExecutionReports: PropTypes.func,
    }) // DI store object that provides the business layer API to DI related services
};

WorkflowReportManager.defaultProps = {
    diStore: silkStore
};
