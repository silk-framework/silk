import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardOptions, CardTitle, IconButton } from "@wrappers/index";
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

export function TaskConfig(props: IProps) {
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    const [labelledTaskData, setLabelledTaskData] = useState<ITaskSchemaAndData>(null);
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const openConfigModal = async () => {
        setLoading(true);
        try {
            // Config dialog is always opened with fresh data
            const taskData = await requestTaskData(props.projectId, props.taskId);
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
            const taskData = await requestTaskData(props.projectId, props.taskId, true);
            const taskDescription = await requestArtefactProperties(taskData.data.type);
            setLabelledTaskData({ taskData, taskDescription });
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        initPreviewData();
    }, [isOpen]);

    return (
        <Card>
            <CardHeader>
                <CardTitle>
                    <h3>Configuration</h3>
                </CardTitle>
                <CardOptions>
                    <IconButton name={"item-edit"} text={"Configure"} onClick={openConfigModal} />
                </CardOptions>
            </CardHeader>
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
