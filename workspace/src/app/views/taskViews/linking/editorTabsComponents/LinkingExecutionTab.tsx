import { Card, IconButton, OverviewItem, Spacing } from "@eccenca/gui-elements";
import { CONTEXT_PATH } from "../../../../constants/path";
import React from "react";
import { useTranslation } from "react-i18next";
import LinkingExecutionReport from "../../../pages/MappingEditor/ExecutionReport/LinkingExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";

//styles
import "./tabs.scss";

interface IProps {
    projectId: string;
    taskId: string;
}
const LinkingExecutionTab = ({ projectId, taskId }: IProps) => {
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
                        registerToReceiveUpdates={handleReceivedUpdates}
                        projectId={projectId}
                        taskId={taskId}
                        activityName="ExecuteLinking"
                        label="Execute Linking"
                    />
                </Card>
                <Spacing size="tiny" vertical />
                <IconButton
                    name="item-download"
                    text={t("common.action.download")}
                    href={`${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/downloadOutput`}
                />
            </OverviewItem>
            <div className="linking-report__wrapper">
                <LinkingExecutionReport project={projectId} task={taskId} updateCounter={executionUpdateCounter} />
            </div>
        </>
    );
};

export default LinkingExecutionTab;
