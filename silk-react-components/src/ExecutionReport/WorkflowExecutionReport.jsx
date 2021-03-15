import React from 'react';
import PropTypes from 'prop-types';
import {Card, CardContent, CardTitle, Icon} from '@eccenca/gui-elements';
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";

/**
 * Displays a workflow execution report.
 */
export default class WorkflowExecutionReport extends React.Component {

  constructor(props) {
    super(props);
    this.displayName = 'WorkflowExecutionReport';
    this.state = {
      executionMetaData: null,
      executionReport: {
        summary: [],
        warnings: [],
        task: {
          id: "workflow"
        },
        taskReports: []
      },
      selectedIndex: -1 // the index of the selected task report or -1 for the workflow itself
    };
  }

  componentDidMount() {
    this.loadExecutionReport();
  }

  componentDidUpdate(prevProps) {
    if (this.props.project !== prevProps.project ||
        this.props.task !== prevProps.task ||
        this.props.time !== prevProps.time) {
      this.loadExecutionReport();
    }
  }

  loadExecutionReport() {
    this.props.diStore.retrieveExecutionReport(
        this.props.baseUrl,
        this.props.project,
        this.props.task,
        this.props.time)
        .then((report) => {
          this.setState({
            executionReport: report.value,
            executionMetaData: report.metaData
          });
        })
        .catch((error) => {
          console.log("Loading execution report failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
        });
  }

  render() {
    return  <div className="mdl-grid mdl-grid--no-spacing">
              <div className="mdl-cell mdl-cell--2-col">
                <Card className="silk-report-card">
                  <CardTitle>
                    Tasks
                  </CardTitle>
                  <CardContent>
                    <ul className="mdl-list">
                      { this.renderTaskItem(this.state.executionReport, -1) }
                      { this.state.executionReport.taskReports.map((report, index) => this.renderTaskItem(report, index)) }
                    </ul>
                  </CardContent>
                </Card>
              </div>
              <div className="mdl-cell mdl-cell--10-col">
                { this.renderReport(this.state.executionReport.nodeId) }
              </div>
            </div>
  }

  renderTaskItem(report, index) {
    return <li key={"report-" + index} className="mdl-list__item mdl-list__item--two-line silk-report-list-item" onClick={() => this.setState({selectedIndex: index})} >
             <span className="mdl-list__item-primary-content">
               { report.label } { (report.task.id !== report.nodeId) ? '(' + report.nodeId + ')' : ''}
               { this.renderTaskDescription(report) }
             </span>
             <span className="mdl-list__item-secondary-content">
               { this.renderTaskIcon(report) }
             </span>
           </li>
  }

  renderTaskDescription(report) {
    if(report.hasOwnProperty("warnings") && report.warnings.length > 0) {
      return <span className="mdl-list__item-sub-title">{report.warnings.length} warnings</span>
    } else {
      return <span className="mdl-list__item-sub-title">no issues</span>
    }
  }

  renderTaskIcon(report) {
    if(report.hasOwnProperty("warnings") && report.warnings.length > 0) {
      return <Icon name="warning" className="silk-report-list-item-icon-red" />
    } else {
      return <Icon name="done" className="silk-report-list-item-icon-green" />
    }
  }

  renderReport(nodeId) {
    if(this.state.selectedIndex >= 0) {
      return <ExecutionReport baseUrl={this.props.baseUrl}
                              project={this.props.project}
                              nodeId={nodeId}
                              executionReport={this.state.executionReport.taskReports[this.state.selectedIndex]}/>
    } else {
      return <ExecutionReport baseUrl={this.props.baseUrl}
                              project={this.props.project}
                              nodeId={this.state.executionReport.task.id}
                              executionReport={this.state.executionReport}
                              executionMetaData={this.state.executionMetaData}/>
    }
  }
}

WorkflowExecutionReport.propTypes = {
  baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
  project: PropTypes.string.isRequired, // project ID
  task: PropTypes.string.isRequired, // task ID
  time: PropTypes.string.isRequired, // timestamp of the current report
  diStore: PropTypes.shape({
    retrieveExecutionReport: PropTypes.func,
  }) // DI store object that provides the business layer API to DI related services
};

WorkflowExecutionReport.defaultProps = {
  diStore: silkStore
};
