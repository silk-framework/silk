import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardOptions, CardTitle, Divider, IconButton } from "@eccenca/gui-elements";
import { useDispatch, useSelector } from "react-redux";
import { commonOp, commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { requestArtefactProperties } from "@ducks/common/requests";
import { Loading } from "../Loading/Loading";
import { TaskConfigPreview } from "./TaskConfigPreview";
import { IProjectTask, TemplateValueType } from "@ducks/shared/typings";
import { IPluginDetails } from "@ducks/common/typings";
import { commonSlice } from "@ducks/common/commonSlice";
import { useTranslation } from "react-i18next";
import useErrorHandler from "../../../hooks/useErrorHandler";

interface IProps {
    projectId: string;
    taskId: string;
    /** Is called with the task data as soon as it is available. */
    pluginDataCallback?: (task: IPluginDetails) => any
}

export interface ITaskSchemaAndData {
    taskData: IProjectTask;
    taskDescription: IPluginDetails;
}

/**
 * Task config widget that shows config options and allows to change them.
 */
export function TaskConfig(props: IProps) {
    const { registerError } = useErrorHandler();
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    const [labelledTaskData, setLabelledTaskData] = useState<ITaskSchemaAndData | undefined>(undefined);
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const { cachedArtefactProperties } = useSelector(commonSel.artefactModalSelector);
    const [t] = useTranslation();

    // Fetch artefact description from cache or fetch and update
    const artefactProperties = async (artefactId: string) => {
        if (cachedArtefactProperties[artefactId]) {
            return cachedArtefactProperties[artefactId];
        } else {
            const taskPluginDetails = await requestArtefactProperties(artefactId);
            dispatch(commonSlice.actions.setCachedArtefactProperty(taskPluginDetails));
            return taskPluginDetails;
        }
    };

    // Open the update modal for the task
    const openConfigModal = async () => {
        setLoading(true);
        try {
            // Config dialog is always opened with fresh data
            const taskData = (await requestTaskData(props.projectId, props.taskId, true)).data;
            const taskPluginDetails = await artefactProperties(taskData.data.type);
            const dataParameters: Record<string, string> | undefined =
                taskPluginDetails.taskType === "Dataset"
                    ? {
                          readOnly: `${taskData.data.readOnly === true}`,
                      }
                    : undefined;
            if (dataParameters && taskData.data.uriProperty) {
                dataParameters.uriProperty = taskData.data.uriProperty;
            }
            const templates: TemplateValueType = taskData.data.templates ?? {};
            dispatch(
                commonOp.updateProjectTask({
                    projectId: taskData.project,
                    taskId: taskData.id,
                    metaData: taskData.metadata,
                    taskPluginDetails: taskPluginDetails,
                    currentParameterValues: taskData.data.parameters,
                    dataParameters: dataParameters,
                    currentTemplateValues: templates,
                })
            );
        } catch (e) {
            registerError("TaskConfig-openConfigModal", "Cannot open edit dialog.", e);
        } finally {
            setLoading(false);
        }
    };

    const initPreviewData = async () => {
        setLoading(true);
        try {
            // Fetch data for preview of config
            const taskData = (await requestTaskData(props.projectId, props.taskId, true)).data;
            if (taskData.data.type) {
                const taskDescription = await artefactProperties(taskData.data.type);
                props.pluginDataCallback?.(taskDescription)
                setLabelledTaskData({ taskData, taskDescription });
            }
        } catch (ex) {
            registerError("TaskConfig-initPreviewData", "Failed to load config data.", ex);
        } finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (!isOpen && taskId) {
            // Always update when the update modal was closed
            initPreviewData();
        }
    }, [taskId, isOpen]);

    let titlePostfix = "";
    if (labelledTaskData) {
        titlePostfix = `: ${t(
            "common.dataTypes." + labelledTaskData.taskDescription.title.toLowerCase(),
            labelledTaskData.taskDescription.title
        )}`;
    }

    // FIXME: CMEM-3742: only return CardContent when it has items, so we need check content before rendering
    return (
        <Card data-test-id={"taskConfigWidget"}>
            <CardHeader>
                <CardTitle>
                    <h2>
                        {t("widget.TaskConfigWidget.title", "Configuration")}
                        {titlePostfix}
                    </h2>
                </CardTitle>
                <CardOptions>
                    <IconButton
                        data-test-id="task-config-edit-btn"
                        name={"item-edit"}
                        text={t("common.action.configure", "Configure")}
                        onClick={openConfigModal}
                    />
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent style={{ maxHeight: "25vh" }}>
                {loading || !labelledTaskData ? (
                    <Loading description={t("widget.TaskConfigWidget.loading", "Loading update dialog...")} />
                ) : (
                    <TaskConfigPreview {...labelledTaskData} />
                )}
            </CardContent>
        </Card>
    );
}
