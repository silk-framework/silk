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
    const openConfigModal = async () => {
        setLoading(true);
        try {
            // Config dialog is always opened with fresh data
            const response = await requestTaskData(props.projectId, props.taskId);
            const taskData = response.data();
            const taskPluginDetails = await requestArtefactProperties(taskData.data.type);
            dispatch(
                commonOp.updateProjectTask({
                    projectId: taskData.project,
                    taskId: taskData.id,
                    metaData: taskData.metadata,
                    taskPluginDetails: taskPluginDetails,
                    currentParameterValues: taskData.data.parameters,
                })
            );
        } finally {
            setLoading(false);
        }
    };

    const initPreviewData = async () => {
        setLoading(true);
        try {
            // Fetch data for preview of config
            const taskData = (await requestTaskData(props.projectId, props.taskId, true)).data();
            const taskDescription = await requestArtefactProperties(taskData.data.type);
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
    }, [isOpen]);

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
