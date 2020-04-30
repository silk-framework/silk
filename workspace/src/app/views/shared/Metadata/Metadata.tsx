import React, { useEffect, useState } from "react";
import { sharedOp } from "@ducks/shared";
import {
    Button,
    Card,
    CardActions,
    CardActionsAux,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    ContextMenu,
    Divider,
    IconButton,
    MenuItem,
    TextArea,
    TextField,
} from "@wrappers/index";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/thunks/metadata.thunk";
import { Loading } from "../Loading/Loading";
import { useForm, Controller } from "react-hook-form";

export function Metadata({ projectId = null, taskId }) {
    const { control, handleSubmit } = useForm();

    const [loading, setLoading] = useState(false);
    const [data, setData] = useState({} as IMetadata);
    const [isEditing, setIsEditing] = useState(false);
    const [errors, setErrors] = useState({
        form: {
            label: false,
        },
        alerts: {},
    });

    const { label, description } = data;

    useEffect(() => {
        getTaskMetadata(taskId, projectId);
    }, [taskId, projectId]);

    const letLoading = async (callback) => {
        setLoading(true);

        const result = await callback();

        setLoading(false);
        return result;
    };

    const toggleEdit = () => {
        setIsEditing(!isEditing);
    };

    const getTaskMetadata = async (taskId: string, projectId: string) => {
        const result = await letLoading(() => {
            return sharedOp.getTaskMetadataAsync(taskId, projectId);
        });
        setData(result);
    };

    const onSubmit = async (inputs: IMetadataUpdatePayload) => {
        if (!inputs.label) {
            return setErrors({
                ...errors,
                form: {
                    label: true,
                },
            });
        }

        setErrors({
            ...errors,
            form: {
                label: false,
            },
        });

        const result = await letLoading(() => {
            return sharedOp.updateTaskMetadataAsync(taskId, inputs, projectId);
        });

        setData(result);

        toggleEdit();
    };

    return (
        <>
            <div className="metadata-block">
                <Card>
                    <CardHeader>
                        <CardTitle>
                            <h4>Details & Metadata</h4>
                        </CardTitle>
                        {!isEditing && (
                            <CardOptions>
                                <IconButton name="item-edit" text="Edit" onClick={toggleEdit} />
                                <ContextMenu>
                                    <MenuItem text={"This"} disabled />
                                    <MenuItem text={"Is just a"} disabled />
                                    <MenuItem text={"Dummy"} disabled />
                                </ContextMenu>
                            </CardOptions>
                        )}
                    </CardHeader>
                    <Divider />
                    {loading ? (
                        <Loading />
                    ) : isEditing ? (
                        <form onSubmit={handleSubmit(onSubmit)}>
                            <CardContent>
                                <Controller as={TextField} name="label" control={control} defaultValue={label} />
                                {errors.form.label && "Label is required"}
                                <Controller
                                    as={TextArea}
                                    name="description"
                                    control={control}
                                    defaultValue={description}
                                />
                            </CardContent>
                            <Divider />
                            <CardActions>
                                <Button affirmative text="Save" type={"submit"} />
                                <Button text="Cancel" onClick={toggleEdit} />
                            </CardActions>
                        </form>
                    ) : (
                        <>
                            <CardContent>
                                <div>
                                    <p>Name: {label}</p>
                                    {!!description && <p>Description: {description}</p>}
                                </div>
                            </CardContent>
                            <Divider />
                            <CardActions>
                                <Button text="Remove me" disruptive />
                                <Button text="Dummy" />
                                <CardActionsAux>
                                    <Button text="Auxiliary action" minimal />
                                </CardActionsAux>
                            </CardActions>
                        </>
                    )}
                </Card>
            </div>
        </>
    );
}
