import React, { useEffect, useState } from "react";
import { sharedOp } from "@ducks/shared";
import { routerOp } from "@ducks/router";
import {
    Button,
    Card,
    CardActions,
    CardContent,
    CardHeader,
    CardOptions,
    CardTitle,
    Divider,
    IconButton,
    TextArea,
    TextField,
} from "@wrappers/index";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/thunks/metadata.thunk";
import { Loading } from "../Loading/Loading";
import { Controller, useForm } from "react-hook-form";
import { Intent } from "@wrappers/blueprint/constants";
import { FormGroup } from "@blueprintjs/core";
import { useLocation } from "react-router";
import { useDispatch } from "react-redux";
import { IPageLabels } from "@ducks/router/operations";

export function Metadata({ projectId = null, taskId }) {
    const { control, handleSubmit } = useForm();

    const [loading, setLoading] = useState(false);
    const [data, setData] = useState({} as IMetadata);
    const [isEditing, setIsEditing] = useState(false);
    const location = useLocation();
    const dispatch = useDispatch();
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

        const updateLocationState = async (forPath: string, metaData: IMetadata) => {
            const newLabels: IPageLabels = {};
            if (projectId) {
                // Project ID exists, this must be a task
                newLabels.taskLabel = metaData.label;
            } else {
                newLabels.projectLabel = metaData.label;
            }
            dispatch(routerOp.replacePage(forPath, newLabels)); // TODO: Since this is executed async this may replace the wrong page history state. How to prevent this?
        };

        const result = await letLoading(() => {
            const path = location.pathname;
            return sharedOp.updateTaskMetadataAsync(taskId, inputs, projectId).then((metaData) => {
                updateLocationState(path, metaData);
                return metaData;
            });
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
                            </CardOptions>
                        )}
                    </CardHeader>
                    <Divider />
                    {loading ? (
                        <Loading />
                    ) : isEditing ? (
                        <form onSubmit={handleSubmit(onSubmit)}>
                            <FormGroup
                                key="label"
                                inline={false}
                                label="Label"
                                labelFor={"label"}
                                labelInfo="(required)"
                            >
                                <Controller
                                    as={TextField}
                                    name="label"
                                    control={control}
                                    defaultValue={label}
                                    intent={errors.form.label ? Intent.DANGER : Intent.NONE}
                                />
                                {errors.form.label && "Label is required"}
                            </FormGroup>
                            <FormGroup key="description" inline={false} label="Description" labelFor={"description"}>
                                <Controller
                                    as={TextArea}
                                    name="description"
                                    control={control}
                                    defaultValue={description}
                                    fullWidth={true}
                                />
                            </FormGroup>
                            <Divider />
                            <CardActions>
                                <Button affirmative text="Save" type={"submit"} />
                                <Button text="Cancel" onClick={toggleEdit} />
                            </CardActions>
                        </form>
                    ) : (
                        <>
                            <CardContent>
                                <div>{!!description && <p>Description: {description}</p>}</div>
                            </CardContent>
                        </>
                    )}
                </Card>
            </div>
        </>
    );
}
