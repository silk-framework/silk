import { Card, Grid, GridColumn, GridRow, IconButton, OverviewItem, Spacing } from "@eccenca/gui-elements";
import { CONTEXT_PATH } from "../../../../constants/path";
import React from "react";
import { useTranslation } from "react-i18next";
import TransformExecutionReport from "../../../../views/pages/MappingEditor/ExecutionReport/TransformExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";
import { checkIfTaskSupportsDownload } from "@ducks/common/requests";

//styles
import { ProjectTaskDownloadInfo } from "@ducks/common/typings";

interface IProps {
    projectId: string;
    taskId: string;
}
const TransformExecutionTab = ({ projectId, taskId }: IProps) => {
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
        <Grid fullWidth>
            <GridRow>
                <OverviewItem hasSpacing>
                    <Spacing size="small" vertical />
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
                    <IconButton
                        name="item-download"
                        text={taskDownloadInfo?.info || t("common.action.download")}
                        disabled={!taskDownloadInfo?.downloadSupported}
                        href={`${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/downloadOutput`}
                    />
                </OverviewItem>
            </GridRow>
            <Spacing size="tiny" vertical />
            <GridRow>
                <GridColumn full>
                    <TransformExecutionReport
                        project={projectId}
                        task={taskId}
                        updateCounter={executionUpdateCounter}
                    />
                </GridColumn>
            </GridRow>
        </Grid>
    );
};

export default TransformExecutionTab;
