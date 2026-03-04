import { Button, Divider, Grid, GridColumn, GridRow, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { CONTEXT_PATH } from "../../../../constants/path";
import React from "react";
import { useTranslation } from "react-i18next";
import LinkingExecutionReport from "../../../pages/MappingEditor/ExecutionReport/LinkingExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";
import { checkIfTaskSupportsDownload } from "@ducks/common/requests";

//styles
import { ProjectTaskDownloadInfo } from "@ducks/common/typings";
import { DatasetClearButton } from "../../shared/dataset/DatasetClearButton";
import { requestTaskData } from "@ducks/shared/requests";
import { ILinkingTaskParameters } from "../linking.types";
import useErrorHandler from "../../../../hooks/useErrorHandler";

interface IProps {
    projectId: string;
    taskId: string;
}
const LinkingExecutionTab = ({ projectId, taskId }: IProps) => {
    const [t] = useTranslation();
    const [executionUpdateCounter, setExecutionUpdateCounter] = React.useState<number>(0);
    const [taskDownloadInfo, setTaskDownloadInfo] = React.useState<ProjectTaskDownloadInfo | undefined>();
    const [outputDatasetId, setOutputDatasetId] = React.useState<string | undefined>(undefined);
    const { registerError } = useErrorHandler();

    const handleActivityUpdates = React.useCallback((status) => {
        if (status.statusName === "Finished") {
            setExecutionUpdateCounter((n) => ++n);
        }
    }, []);

    const fetchOutputInfo = async () => {
        try {
            const linkingTask = (await requestTaskData<ILinkingTaskParameters>(projectId, taskId)).data;
            const outputTaskId = linkingTask.data.parameters.output;
            if (outputTaskId) {
                setOutputDatasetId(typeof outputTaskId === "string" ? outputTaskId : outputTaskId.value);
            }
        } catch (error) {
            registerError("LinkingExecutionTab.fetchOutputInfo", "Could not fetch linking task data.", error);
        }
    };

    React.useEffect(() => {
        (async () => {
            try {
                fetchOutputInfo();
                const response = await checkIfTaskSupportsDownload(projectId, taskId);
                setTaskDownloadInfo(response.data);
            } catch (err) {}
        })();
    }, [projectId, taskId]);

    return (
        <Grid>
            <GridRow>
                <GridColumn>
                    <Toolbar noWrap>
                        <ToolbarSection canGrow canShrink />
                        {outputDatasetId ? (
                            <DatasetClearButton projectId={projectId} datasetId={outputDatasetId} />
                        ) : null}
                        <ToolbarSection canShrink>
                            <TaskActivityWidget
                                updateCallback={handleActivityUpdates}
                                projectId={projectId}
                                taskId={taskId}
                                activityName="ExecuteLinking"
                                label="Execute Linking"
                                layoutConfig={{
                                    border: true,
                                    small: true,
                                    hasSpacing: true,
                                    canShrink: true,
                                }}
                            />
                        </ToolbarSection>
                        <ToolbarSection>
                            <Spacing vertical size="small" />
                            <Button
                                text={t("common.action.download")}
                                tooltip={taskDownloadInfo?.info || undefined}
                                disabled={!taskDownloadInfo?.downloadSupported}
                                href={`${CONTEXT_PATH}/workspace/projects/${projectId}/tasks/${taskId}/downloadOutput`}
                            />
                        </ToolbarSection>
                    </Toolbar>
                    <Divider addSpacing="small" />
                    <Spacing size="small" />
                </GridColumn>
            </GridRow>
            <Spacing size="large" />
            <GridRow>
                <GridColumn>
                    <LinkingExecutionReport project={projectId} task={taskId} updateCounter={executionUpdateCounter} />
                </GridColumn>
            </GridRow>
        </Grid>
    );
};

export default LinkingExecutionTab;
