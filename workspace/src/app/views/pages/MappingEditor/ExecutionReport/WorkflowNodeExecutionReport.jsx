import React from "react";
import PropTypes from "prop-types";
import { ContentGroup, Spacing } from "@eccenca/gui-elements";
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
        return this.state.executionReports.map((report, index) => {
            const reportTitle = this.reportTitle(report);
            const title =
                this.state.executionReports.length > 1
                    ? `Report ${index + 1} of ${this.state.executionReports.length} — ${reportTitle}`
                    : reportTitle;
            if ("taskReports" in report) {
                // This is a nested workflow execution report
                return (
                    <React.Fragment key={`${report.nodeId ?? this.props.nodeId}-${index}`}>
                        <ContentGroup title={title} whitespaceSize="medium">
                            <div style={{ position: "relative", height: "100%" }}>
                                <WorkflowExecutionReport project={this.props.project} executionReport={report} />
                            </div>
                        </ContentGroup>
                        {index < this.state.executionReports.length - 1 && <Spacing size="medium" />}
                    </React.Fragment>
                );
            } else {
                return (
                    <React.Fragment key={`${report.nodeId ?? this.props.nodeId}-${index}`}>
                        <ContentGroup title={title} whitespaceSize="medium">
                            <ExecutionReport
                                project={this.props.project}
                                nodeId={this.props.nodeId}
                                executionReport={report}
                                trackRuleInUrl={false}
                            />
                        </ContentGroup>
                        {index < this.state.executionReports.length - 1 && <Spacing size="medium" />}
                    </React.Fragment>
                );
            }
        });
    }

    reportTitle(report) {
        if (report.title) {
            return report.title;
        }

        const operation = report.operation ? `${report.operation}: ` : "";
        return `${report.label ?? "Execution"} · ${operation}${report.entityCount ?? 0} ${report.operationDesc ?? "entities processed"}`;
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
