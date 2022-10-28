import { Card, IconButton, OverviewItem, Spacing } from "@eccenca/gui-elements";
import { CONTEXT_PATH } from "../../../../constants/path";
import React from "react";
import { useTranslation } from "react-i18next";
import LinkingExecutionReport from "../../../pages/MappingEditor/ExecutionReport/LinkingExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";
import { checkIfTaskSupportsDownload } from "@ducks/common/requests";

//styles
import "./tabs.scss";
import { ProjectTaskDownloadInfo } from "@ducks/common/typings";

interface IProps {
    projectId: string;
    taskId: string;
}
const LinkingExecutionTab = ({ projectId, taskId }: IProps) => {
    const [t] = useTranslation();
    const [executionUpdateCounter, setExecutionUpdateCounter] = React.useState<number>(0);
    const [taskDownloadInfo, setTaskDownloadInfo] = React.useState<ProjectTaskDownloadInfo | undefined>();

    const handleReceivedUpdates = React.useCallback((status) => {
        if (status.statusName === "Finished") {
            setExecutionUpdateCounter((n) => ++n);
        }
    }, []);

    React.useEffect(() => {
        (async () => {
            try {
                const response = await checkIfTaskSupportsDownload(projectId, taskId);
                setTaskDownloadInfo(response.data);
            } catch (err) {}
        })();
    }, [projectId, taskId]);

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
                    text={taskDownloadInfo?.info || t("common.action.download")}
                    disabled={!taskDownloadInfo?.downloadSupported}
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
