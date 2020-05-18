import React, { useState } from "react";
import { Card, CardContent, CardHeader, CardOptions, CardTitle, IconButton } from "@wrappers/index";
import { useDispatch } from "react-redux";
import { commonOp } from "@ducks/common";
import Spinner from "@wrappers/blueprint/spinner";
import { requestTaskData } from "@ducks/shared/requests";
import { requestArtefactProperties } from "@ducks/common/requests";

interface IProps {
    projectId: string;
    taskId: string;
}

export function TaskConfig(props: IProps) {
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    const openConfigModal = async () => {
        setLoading(true);
        try {
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
            <CardContent>{loading ? <Spinner /> : <p>TODO: Add preview of config?</p>}</CardContent>
        </Card>
    );
}
