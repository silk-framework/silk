import { Divider, Grid, GridColumn, GridRow, Button, Spacing, Toolbar, ToolbarSection } from "@eccenca/gui-elements";
import { CONTEXT_PATH } from "../../../../constants/path";
import React from "react";
import { useTranslation } from "react-i18next";
import TransformExecutionReport from "../../../../views/pages/MappingEditor/ExecutionReport/TransformExecutionReport";
import { TaskActivityWidget } from "../../../shared/TaskActivityWidget/TaskActivityWidget";
import { checkIfTaskSupportsDownload } from "@ducks/common/requests";
import { IViewActions } from "../../../../views/plugins/PluginRegistry";

//styles
import { ProjectTaskDownloadInfo } from "@ducks/common/typings";
import { DatasetClearButton } from "../../shared/dataset/DatasetClearButton";
import { requestTaskData } from "@ducks/shared/requests";
import { ITransformTaskParameters } from "../transform.types";
import useErrorHandler from "../../../../hooks/useErrorHandler";

interface IProps {
    projectId: string;
    taskId: string;
    viewActions?: IViewActions;
}
const TransformExecutionTab = ({ projectId, taskId, viewActions }: IProps) => {
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
            const transformTask = (await requestTaskData<ITransformTaskParameters>(projectId, taskId)).data;
            const outputTaskId = transformTask.data.parameters.output;
            if (outputTaskId) {
                setOutputDatasetId(outputTaskId);
            }
        } catch (error) {
            registerError("TransformExecutionTab.fetchOutputInfo", "Could not fetch transform task data.", error);
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
        if (viewActions?.addLocalBreadcrumbs) {
            viewActions.addLocalBreadcrumbs([]);
        }
    }, [projectId, taskId]);

    return (
        <div>
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
                                    projectId={projectId}
                                    taskId={taskId}
                                    activityName="ExecuteTransform"
                                    updateCallback={handleActivityUpdates}
                                    label="Execute Transform"
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
                <GridRow>
                    <GridColumn>
                        <TransformExecutionReport
                            project={projectId}
                            task={taskId}
                            updateCounter={executionUpdateCounter}
                        />
                    </GridColumn>
                </GridRow>
            </Grid>
        </div>
    );
};

export default TransformExecutionTab;
