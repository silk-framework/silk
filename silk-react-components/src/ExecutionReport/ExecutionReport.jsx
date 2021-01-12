import React from 'react';
import PropTypes from 'prop-types';
import { Card, CardContent, CardTitle, Table, TableBody, TableCell, TableHead, TableRow } from '@eccenca/gui-elements';
import MappingsTree from '../HierarchicalMapping/containers/MappingsTree';

/**
 * Displays a task execution report.
 */
export default class ExecutionReport extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = 'ExecutionReport';
        this.state = {
            currentRuleId: null,
        };
        this.onRuleNavigation = this.onRuleNavigation.bind(this);
    }
    
    onRuleNavigation({newRuleId}) {
        this.setState({currentRuleId: newRuleId});
    }
    
    renderSummary() {
        const summaryRows = this.props.executionReport.summary.map(v =>
            <tr key={v.key}>
                <td className="silk-report-table-bold">{v.key}</td>
                <td>{v.value}</td>
            </tr>
        );
        return <Card className="silk-report-card">
            <CardTitle>
                Execution Report
            </CardTitle>
            <CardContent>
                <div>
                    <table className="silk-report-table">
                        <thead>
                        </thead>
                        <tbody>
                        {summaryRows}
                        </tbody>
                    </table>
                </div>
                {this.renderWarning()}
            </CardContent>
        </Card>
    }

    renderWarning() {
        if(this.props.executionReport.warnings.length > 0) {
            const warnings = this.props.executionReport.warnings.map(warning =>
                <div className="mdl-alert mdl-alert--info mdl-alert--border mdl-alert--spacing">
                    <div className="mdl-alert__content">
                        <p>{warning}</p>
                    </div>
                </div>
            );

            return <div className="silk-report-warning">{warnings}</div>
        } else {
            return <div className="silk-report-warning">
                        <div className="mdl-alert mdl-alert--info mdl-alert--border mdl-alert--spacing">
                            <div className="mdl-alert__content">
                                <p>Task '{this.props.executionReport.label}' has been executed without any issues.</p>
                            </div>
                        </div>
                   </div>
        }
    }
    
    renderTransformReport() {
        return <div className="mdl-grid mdl-grid--no-spacing">
            <div className="mdl-cell mdl-cell--3-col">
                <MappingsTree
                    currentRuleId="root"
                    ruleTree={this.props.executionReport.task.data.parameters.mappingRule}
                    showValueMappings={true}
                    handleRuleNavigation={this.onRuleNavigation}
                    ruleValidation={this.generateIcons()}
                />
            </div>
            <div className="mdl-cell mdl-cell--9-col">
                {this.renderRuleReport()}
            </div>
        </div>
    }
    
    generateIcons() {
        let ruleIcons = {};
        for (let [ruleId, ruleResults] of Object.entries(this.props.executionReport.ruleResults)) {
            if (ruleResults.errorCount === 0) {
                ruleIcons[ruleId] = "ok"
            } else {
                ruleIcons[ruleId] = "warning"
            }
        }
        return ruleIcons
    }
    
    renderRuleReport() {
        const ruleResults = this.props.executionReport.ruleResults[this.state.currentRuleId];
        let title;
        if (ruleResults === undefined) {
            title = "Select a mapping for detailed results."
        } else if (ruleResults.errorCount === 0) {
            title = "This mapping executed successfully without any issues."
        } else {
            title = "This mapping generated  " + ruleResults.errorCount + " validation issues during execution."
        }
        return <div className="ecc-silk-mapping__treenav">
            <Card className="mdl-card mdl-shadow--2dp mdl-card--stretch">
                <div className="mdl-card__supporting-text">
                    {title}
                </div>
                {ruleResults !== undefined && ruleResults.errorCount > 0 && this.renderRuleErrors(ruleResults)}
            </Card>
        </div>
    }
    
    renderRuleErrors(ruleResults) {
        return <Table className="di-execution-report-table" style={{width: "100%"}}>
            <TableHead>
                <TableRow>
                    <TableCell isHead={true}>Entity</TableCell>
                    <TableCell isHead={true}>Values</TableCell>
                    <TableCell isHead={true}>Issue</TableCell>
                </TableRow>
            </TableHead>
            <TableBody>
                {ruleResults.sampleErrors.map(this.renderRuleError)}
            </TableBody>
        </Table>
    }
    
    renderRuleError(ruleError) {
        return <TableRow key={ruleError.entity}>
            <TableCell><div className="silk-report-errors-value">{ruleError.entity}</div></TableCell>
            <TableCell><div className="silk-report-errors-value">{ruleError.values.flat().join(', ')}</div></TableCell>
            <TableCell><div className="silk-report-errors-value">{ruleError.error}</div></TableCell>
        </TableRow>
    }
    
    render() {
        return <div>
            {this.renderSummary()}
            {'ruleResults' in this.props.executionReport && this.renderTransformReport()}
        </div>
    }
}

ExecutionReport.propTypes = {
    baseUrl: PropTypes.string.isRequired, // Base URL of the DI service
    project: PropTypes.string.isRequired, // project ID
    nodeId: PropTypes.string.isRequired, // workflow node ID
    executionReport: PropTypes.object // The transform execution report to render
};
