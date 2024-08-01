import React from "react";
import silkStore from "../api/silkStore";
import ExecutionReport from "./ExecutionReport";
import {ExecutionReportProps, ExecutionReportResponse} from "./report-typings";

/**
 * Displays a transform task execution report.
 */
const TransformExecutionReport: React.FC<ExecutionReportProps> = ({
    project,
    task,
    updateCounter,
    diStore = silkStore,
}) => {
    const [executionReport, setExecutionReport] = React.useState<ExecutionReportResponse | undefined>();

    const updateExecutionReport = () => {
        diStore
            .getTransformExecutionReport(project, task)
            .then((report) => setExecutionReport(report))
            .catch((error) => {
                console.log("Loading execution report failed! " + error); // FIXME: Handle error and give user feedback. Currently this is done via the activity status widget
            });
    };

    React.useEffect(() => {
        updateExecutionReport();
    }, [updateCounter]);

    return <ExecutionReport executionReport={executionReport} />;
};

export default TransformExecutionReport;
