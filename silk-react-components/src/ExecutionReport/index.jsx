import React from 'react';
import PropTypes from 'prop-types';
import {AffirmativeButton, DismissiveButton, SelectBox, Info, Spinner, Error, Table} from '@eccenca/gui-elements';
import dataIntegrationStore from "../api/dataintegrationStore";
import MappingsTree from '../HierarchicalMapping/Components/MappingsTree';
import hierarchicalMappingChannel from "../HierarchicalMapping/store";

/**
 * Displays a task execution report.
 */
export default class ExecutionReportView extends React.Component {

  constructor(props) {
    super(props);
    this.displayName = 'ExecutionReportView';
    this.state = {
      executionReport: {
        summary: []
      },
      currentRuleId: "direct"
    };

    // MappingsTree uses the message bus, so we need to set required Silk properties
    hierarchicalMappingChannel.subject('setSilkDetails').onNext({
      baseUrl: this.props.baseUrl,
      project: this.props.project,
      transformTask: this.props.task
    });

    hierarchicalMappingChannel.subject('ruleId.change').subscribe(({newRuleId, parent})=> {
      this.setState({ currentRuleId: newRuleId });
    });
  }

  componentDidMount() {
    this.props.diStore.getExecutionReport(
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
    if('ruleResults' in this.state.executionReport) {
      return this.renderTransformReport();
    } else {
      return this.renderSummary();
    }
  }

  renderSummary() {
    const summaryRows = this.state.executionReport.summary.map(v =>
        <tr key={v.key}>
          <td>{v.key}</td>
          <td>{v.value}</td>
        </tr>
    );
    return <table className="mdl-data-table mdl-js-data-table">
             <thead>
               <tr>
                 <th>Summary</th>
                 <th></th>
               </tr>
             </thead>
             <tbody>
             { summaryRows }
             </tbody>
           </table>
  }

  renderTransformReport() {
    return <div className=".ecc-silk-mapping">
            <div className="mdl-grid">
              <div className="mdl-cell mdl-cell--2-col">
                { this.renderSummary() }
              </div>
              <div className="mdl-cell mdl-cell--3-col">
                <MappingsTree currentRuleId="root" showValueMappings={true} ruleIcons={this.generateIcons()} />
              </div>
              <div className="mdl-cell mdl-cell--7-col">
                { this.renderRuleReport() }
              </div>
           </div>
         </div>
  }

  generateIcons() {
    let ruleIcons = {};
    for(let [ruleId, ruleResults] of Object.entries(this.state.executionReport.ruleResults)) {
      if(ruleResults.errorCount > 0) {
        ruleIcons[ruleId] = {
          className: "ecc-silk-mapping__ruleitem-icon-red",
          name: "warning",
          tooltip: "warning"
        }
      }
    }
    return ruleIcons
  }

  renderRuleReport() {
    const ruleResults = this.state.executionReport.ruleResults[this.state.currentRuleId];
    return <div>
             Total error count: { ruleResults.errorCount }
             { ruleResults.errorCount > 0 && this.renderRuleErrors(ruleResults) }
           </div>
  }

  renderRuleErrors(ruleResults) {
    return  <table className="mdl-data-table mdl-js-data-table">
              <thead>
              <tr>
                <th>Entity</th>
                <th>Values</th>
                <th>Issue</th>
              </tr>
              </thead>
              <tbody>
              { ruleResults.sampleErrors.map(this.renderRuleError) }
              </tbody>
            </table>
  }

  renderRuleError(ruleError) {
    return <tr key={ruleError.entity}>
             <td>{ruleError.entity}</td>
             <td>{ruleError.values.flat().join(', ')}</td>
             <td>{ruleError.error}</td>
           </tr>
  }
}

ExecutionReportView.propTypes = {
  baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
  project: PropTypes.string.isRequired, // project ID
  task: PropTypes.string.isRequired, // task ID
  diStore: PropTypes.shape({
    getExecutionReport: PropTypes.func,
  }) // DI store object that provides the business layer API to DI related services
};

ExecutionReportView.defaultProps = {
  diStore: dataIntegrationStore
};
