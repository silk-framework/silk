import React from 'react';
import PropTypes from 'prop-types';
import {AffirmativeButton, DismissiveButton, SelectBox, Info, Spinner, Error, Table} from '@eccenca/gui-elements';
import silkStore from "../api/silkStore";
import MappingsTree from '../HierarchicalMapping/Components/MappingsTree';
import hierarchicalMappingChannel from "../HierarchicalMapping/store";
import ExecutionReport from "./ExecutionReport";

/**
 * Displays a transform task execution report.
 */
export default class TransformExecutionReport extends React.Component {

  constructor(props) {
    super(props);
    this.displayName = 'TransformExecutionReport';
    this.state = {
      executionReport: {
        summary: []
      }
    };
  }

  componentDidMount() {
    this.props.diStore.getTransformExecutionReport(
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
    return <ExecutionReport baseUrl={this.props.baseUrl}
                            project={this.props.project}
                            task={this.props.task}
                            executionReport={this.state.executionReport} />
  }
}

TransformExecutionReport.propTypes = {
  baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
  project: PropTypes.string.isRequired, // project ID
  task: PropTypes.string.isRequired, // task ID
  diStore: PropTypes.shape({
    getExecutionReport: PropTypes.func,
  }) // DI store object that provides the business layer API to DI related services
};

TransformExecutionReport.defaultProps = {
  diStore: silkStore
};
