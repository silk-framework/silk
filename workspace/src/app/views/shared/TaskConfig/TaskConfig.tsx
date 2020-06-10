import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardOptions, CardTitle, Divider, IconButton } from "@wrappers/index";
import { useDispatch, useSelector } from "react-redux";
import { commonOp, commonSel } from "@ducks/common";
import { requestTaskData } from "@ducks/shared/requests";
import { requestArtefactProperties } from "@ducks/common/requests";
import { Loading } from "../Loading/Loading";
import { TaskConfigPreview } from "./TaskConfigPreview";
import { IProjectTask } from "@ducks/shared/typings";
import { IDetailedArtefactItem } from "@ducks/common/typings";
import { commonSlice } from "@ducks/common/commonSlice";

interface IProps {
    projectId: string;
    taskId: string;
}

export interface ITaskSchemaAndData {
    taskData: IProjectTask;
    taskDescription: IDetailedArtefactItem;
}

/**
 * Task config widget that shows config options and allows to change them.
 */
export function TaskConfig(props: IProps) {
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    const [labelledTaskData, setLabelledTaskData] = useState<ITaskSchemaAndData>(null);
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const taskId = useSelector(commonSel.currentTaskIdSelector);
    const { cachedArtefactProperties } = useSelector(commonSel.artefactModalSelector);

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
            const taskData = await requestTaskData(props.projectId, props.taskId, true);
            const taskPluginDetails = await artefactProperties(taskData.data.type);
            dispatch(
                commonOp.updateProjectTask({
                    projectId: taskData.project,
                    taskId: taskData.id,
                    metaData: taskData.metadata,
                    taskPluginDetails: taskPluginDetails,
                    currentParameterValues: taskData.data.parameters,
                })
            );
        } catch (e) {
            console.log(e);
        } finally {
            setLoading(false);
        }
    };

    const initPreviewData = async () => {
        setLoading(true);
        try {
            // Fetch data for preview of config
            const taskData = await requestTaskData(props.projectId, props.taskId, true);
            const taskDescription = await artefactProperties(taskData.data.type);
            setLabelledTaskData({ taskData, taskDescription });
        } finally {
            setLoading(false);
        }
    };
    useEffect(() => {
        if (!isOpen) {
            // Always update when the update modal was closed
            initPreviewData();
        }
    }, [taskId, isOpen]);

    let titlePostfix = "";
    if (labelledTaskData) {
        titlePostfix = `: ${labelledTaskData.taskDescription.title}`;
    }

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h3>Configuration{titlePostfix}</h3>
                </CardTitle>
                <CardOptions>
                    <IconButton name={"item-edit"} text={"Configure"} onClick={openConfigModal} />
                </CardOptions>
            </CardHeader>
            <Divider />
            <CardContent>
                {loading ? (
                    <Loading description={"Loading update dialog..."} />
                ) : (
                    <TaskConfigPreview {...labelledTaskData} />
                )}
            </CardContent>
        </Card>
    );
}
