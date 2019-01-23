import React from 'react';
import PropTypes from 'prop-types';
import {AffirmativeButton, DismissiveButton, SelectBox, Info, Spinner, Error, Table} from '@eccenca/gui-elements';
import dataIntegrationStore from "../api/dataintegrationStore";
import MappingsTree from '../HierarchicalMapping/Components/MappingsTree';
import hierarchicalMappingChannel from "../HierarchicalMapping/store";
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
                <div className="silk-report-card mdl-card mdl-shadow--2dp mdl-card--stretch">
                  <ul className="mdl-list">
                    { Object.entries(this.state.executionReport.taskReports).map(e => this.renderTaskItem(e[0], e[1])) }
                  </ul>
                </div>
              </div>
              <div className="mdl-cell mdl-cell--10-col">
                { this.state.executionReport.taskReports.hasOwnProperty(this.state.selectedTask) &&
                  this.renderReport(this.state.selectedTask)
                }
              </div>
            </div>
  }

  renderTaskItem(task, report) {
    return <li key={task} className="mdl-list__item">
             <span className="mdl-list__item-primary-content">
               <button onClick={() => this.setState({selectedTask: task})} className="mdl-button mdl-js-button">
                 { report.label }
               </button>
             </span>
           </li>
  }

  renderReport(task) {
    return <ExecutionReport baseUrl={this.props.baseUrl}
                            project={this.props.project}
                            task={task}
                            executionReport={this.state.executionReport.taskReports[this.state.selectedTask]} />
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
  diStore: dataIntegrationStore
};
