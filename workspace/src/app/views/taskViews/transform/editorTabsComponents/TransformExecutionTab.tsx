import { Card, IconButton, OverviewItem, Spacing } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import TransformExecutionReport from "../../../../views/pages/MappingEditor/ExecutionReport/TransformExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";

//styles
import "./tabs.scss";

interface IProps {
    projectId: string;
    taskId: string;
}
const TransformExecutionTab = ({ projectId, taskId }: IProps) => {
    const [t] = useTranslation();
    const [executionUpdateCounter, setExecutionUpdateCounter] = React.useState<number>(0);

    const handleReceivedUpdates = React.useCallback((status) => {
        if (status.statusName === "Finished") {
            setExecutionUpdateCounter((n) => ++n);
        }
    }, []);

    return (
        <>
            <OverviewItem hasSpacing>
                <Card>
                    <TaskActivityWidget
                        projectId={projectId}
                        taskId={taskId}
                        activityName="ExecuteTransform"
                        registerToReceiveUpdates={handleReceivedUpdates}
                        label="Execute Transform"
                    />
                </Card>
                <Spacing size="tiny" vertical />
                <IconButton name="item-download" text={t("common.action.download")} />
            </OverviewItem>
            <div className="transform-report__wrapper">
                <TransformExecutionReport project={projectId} task={taskId} updateCounter={executionUpdateCounter} />
            </div>
        </>
    );
};

export default TransformExecutionTab;
