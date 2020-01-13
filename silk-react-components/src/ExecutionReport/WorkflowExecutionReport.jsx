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
      executionReport: {
        taskReports: {}
      },
      selectedTask: null
    };
  }

  componentDidMount() {
    this.props.diStore.getWorkflowExecutionReport(
      this.props.baseUrl,
      this.props.project,
      this.props.task)
      .then((report) => {
        this.setState({
          executionReport: report
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
                      { Object.entries(this.state.executionReport.taskReports).map(e => this.renderTaskItem(e[0], e[1])) }
                    </ul>
                  </CardContent>
                </Card>
              </div>
              <div className="mdl-cell mdl-cell--10-col">
                { this.renderReport(this.state.selectedTask) }
              </div>
            </div>
  }

  renderTaskItem(task, report) {
    return <li key={task} className="mdl-list__item mdl-list__item--two-line silk-report-list-item" onClick={() => this.setState({selectedTask: task})} >
             <span className="mdl-list__item-primary-content">
               { report.label }
               { this.renderTaskDescription(task, report) }
             </span>
             <span className="mdl-list__item-secondary-content">
               { this.renderTaskIcon(task, report) }
             </span>
           </li>
  }

  renderTaskDescription(task, report) {
    if(report.hasOwnProperty("warnings") && report.warnings.length > 0) {
      return <span className="mdl-list__item-sub-title">{report.warnings.length} warnings</span>
    } else {
      return <span className="mdl-list__item-sub-title">no issues</span>
    }
  }

  renderTaskIcon(task, report) {
    if(report.hasOwnProperty("warnings") && report.warnings.length > 0) {
      return <Icon name="warning" className="silk-report-list-item-icon-red" />
    } else {
      return <Icon name="done" className="silk-report-list-item-icon-green" />
    }
  }

  renderReport(task) {
    if(this.state.executionReport.taskReports.hasOwnProperty(this.state.selectedTask)) {
      return <ExecutionReport baseUrl={this.props.baseUrl}
                              project={this.props.project}
                              task={task}
                              executionReport={this.state.executionReport.taskReports[this.state.selectedTask]}/>
    } else {
      return  <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
                <div className="mdl-card__supporting-text">
                  Select a task for detailed results.
                </div>
              </div>
    }
  }
}

WorkflowExecutionReport.propTypes = {
  baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
  project: PropTypes.string.isRequired, // project ID
  task: PropTypes.string.isRequired, // task ID
  diStore: PropTypes.shape({
    getExecutionReport: PropTypes.func,
  }) // DI store object that provides the business layer API to DI related services
};

WorkflowExecutionReport.defaultProps = {
  diStore: silkStore
};
