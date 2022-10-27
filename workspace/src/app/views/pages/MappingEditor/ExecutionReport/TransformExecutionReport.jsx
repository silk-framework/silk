import React from "react";
import PropTypes from "prop-types";
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";

/**
 * Displays a transform task execution report.
 */
export default class TransformExecutionReport extends React.Component {
    constructor(props) {
        super(props);
        this.displayName = "TransformExecutionReport";
        this.updateExecutionReport = this.updateExecutionReport.bind(this);
        this.state = {
            executionReport: {
                task: {
                    id: props.task,
                },
                summary: [],
                warnings: [],
            },
        };
    }

    updateExecutionReport() {
        this.props.diStore
            .getTransformExecutionReport(this.props.project, this.props.task)
            .then((report) => {
                this.setState({
                    executionReport: report,
                });
            })
            .catch((error) => {
                console.log("Loading execution report failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
            });
    }

    componentDidMount() {
        this.updateExecutionReport();
    }

    componentDidUpdate(prevProps, prevState) {
        if (prevProps.updateCounter !== this.props.updateCounter) {
            this.updateExecutionReport();
        }
    }
    render() {
        return (
            <ExecutionReport
                project={this.props.project}
                nodeId={this.props.task}
                executionReport={this.state.executionReport}
            />
        );
    }
}

TransformExecutionReport.propTypes = {
    project: PropTypes.string.isRequired, // project ID
    task: PropTypes.string.isRequired, // task ID
    updateCounter: PropTypes.number,
    diStore: PropTypes.shape({
        getExecutionReport: PropTypes.func,
    }), // DI store object that provides the business layer API to DI related services
};

TransformExecutionReport.defaultProps = {
    diStore: silkStore,
};
