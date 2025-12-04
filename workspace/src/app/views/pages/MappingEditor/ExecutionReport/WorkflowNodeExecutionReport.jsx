import React from "react";
import PropTypes from "prop-types";
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";
import WorkflowExecutionReport from "./WorkflowExecutionReport";

/**
 * Displays execution reports for a workflow node.
 */
export default class WorkflowNodeExecutionReport extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = "WorkflowNodeExecutionReport";
        this.state = {
            executionReports: [
                {
                    task: {
                        id: props.task,
                    },
                    summary: [],
                    warnings: [],
                },
            ],
        };
    }

    componentDidMount() {
        this.props.diStore
            .getWorkflowNodeExecutionReports(this.props.project, this.props.task, this.props.nodeId)
            .then((reports) => {
                this.setState({
                    executionReports: reports,
                });
            })
            .catch((error) => {
                console.log("Loading execution report failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
            });
    }

    render() {
        return this.state.executionReports.map((report) => {
            if ("taskReports" in report) {
                // This is a nested workflow execution report
                return (
                    <div style={{ position: "relative", height: "100%" }}>
                        <WorkflowExecutionReport project={this.props.project} executionReport={report} />
                    </div>
                );
            } else {
                return (
                    <ExecutionReport
                        project={this.props.project}
                        nodeId={this.props.nodeId}
                        executionReport={report}
                        trackRuleInUrl={false}
                    />
                );
            }
        });
    }
}

WorkflowNodeExecutionReport.propTypes = {
    project: PropTypes.string.isRequired, // project ID
    task: PropTypes.string.isRequired, // task ID
    nodeId: PropTypes.string.isRequired, // node ID
    diStore: PropTypes.shape({
        getWorkflowNodeExecutionReports: PropTypes.func,
    }), // DI store object that provides the business layer API to DI related services
};

WorkflowNodeExecutionReport.defaultProps = {
    diStore: silkStore,
};
