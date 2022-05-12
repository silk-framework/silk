import React from 'react';
import PropTypes from 'prop-types';
import {Card, CardContent, CardTitle, Icon} from 'gui-elements-deprecated';
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
      selectedIndex: -1 // the index of the selected task report or -1 for the workflow itself
    };
  }

  render() {
      let executionWarnings = [];
      if(this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.cancelled) {
        executionWarnings = [ "Executed cancelled" ]
      } else if(this.props.executionMetaData != null && this.props.executionMetaData.finishStatus.failed) {
        executionWarnings = [ "Executed failed" ]
      }

      return  <div className="mdl-grid mdl-grid--no-spacing">
                <div className="mdl-cell mdl-cell--2-col silk-report-sidebar">
                  <Card className="silk-report-sidebar-overview">
                    <CardTitle>
                      Workflow
                    </CardTitle>
                    <CardContent>
                      <ul className="mdl-list">
                        { this.renderTaskItem(this.props.executionReport, -1, executionWarnings) }
                      </ul>
                    </CardContent>
                  </Card>
                  <Card className="silk-report-sidebar-tasks">
                    <CardTitle className="silk-report-sidebar-tasks-title">
                      Tasks
                    </CardTitle>
                    <CardContent className="silk-report-sidebar-tasks-content">
                      <ul className="mdl-list">
                        { this.props.executionReport.taskReports.map((report, index) => this.renderTaskItem(report, index, report.warnings)) }
                      </ul>
                    </CardContent>
                  </Card>
                </div>
                <div className="mdl-cell mdl-cell--10-col">
                  { this.renderReport(this.props.executionReport.nodeId) }
                </div>
              </div>
  }

  renderTaskItem(report, index, warnings) {
    let classNames = "mdl-list__item mdl-list__item--two-line silk-report-list-item"
    if(index === this.state.selectedIndex) {
      classNames += " silk-report-list-item-icon-selected"
    }

    return <li key={"report-" + index} className={classNames} onClick={() => this.setState({selectedIndex: index})} >
             <span className="mdl-list__item-primary-content">
               { report.label } { (report.operation != null) ? '(' + report.operation + ')' : ''}
               { this.renderTaskDescription(warnings) }
             </span>
             <span className="mdl-list__item-secondary-content">
               { this.renderTaskIcon(warnings, report.error) }
             </span>
           </li>
  }

  renderTaskDescription(warnings) {
    if(warnings != null && warnings.length > 0) {
      return <span className="mdl-list__item-sub-title">{warnings.length} warnings</span>
    } else {
      return <span className="mdl-list__item-sub-title">no issues</span>
    }
  }

  renderTaskIcon(warnings, error) {
    if(error) {
      return <Icon name="danger" className="silk-report-list-item-icon-red" />
    } else if(warnings != null && warnings.length > 0) {
      return <Icon name="warning" className="silk-report-list-item-icon-yellow" />
    } else {
      return <Icon name="done" className="silk-report-list-item-icon-green" />
    }
  }

  renderReport(nodeId) {
    if(this.state.selectedIndex >= 0) {
      const taskReport = this.props.executionReport.taskReports[this.state.selectedIndex];
      if ('taskReports' in taskReport) {
        // This is a nested workflow execution report
        return <WorkflowExecutionReport baseUrl={this.props.baseUrl}
                                        project={this.props.project}
                                        executionReport={taskReport} />
      } else {
        // Render the report of the selected task
        return <ExecutionReport baseUrl={this.props.baseUrl}
                                project={this.props.project}
                                nodeId={nodeId}
                                executionReport={taskReport}/>
    }
    } else {
      // Render the report of the workflow itself
      return <ExecutionReport baseUrl={this.props.baseUrl}
                              project={this.props.project}
                              nodeId={this.props.executionReport.task.id}
                              executionReport={this.props.executionReport}
                              executionMetaData={this.props.executionMetaData}/>
    }
  }
}

WorkflowExecutionReport.propTypes = {
  baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
  project: PropTypes.string.isRequired, // project ID
  executionMetaData: PropTypes.object,
  executionReport: PropTypes.object.isRequired,
  diStore: PropTypes.shape({
    retrieveExecutionReport: PropTypes.func,
  }) // DI store object that provides the business layer API to DI related services
};

WorkflowExecutionReport.defaultProps = {
  diStore: silkStore
};
