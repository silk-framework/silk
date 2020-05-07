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
    FieldItem,
    IconButton,
    TextArea,
    TextField,
} from "@wrappers/index";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/thunks/metadata.thunk";
import { Loading } from "../Loading/Loading";
import { Controller, useForm } from "react-hook-form";
import { Intent } from "@wrappers/blueprint/constants";
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
            if (window.location.pathname.endsWith(forPath)) {
                // Only replace page if still on the same page
                dispatch(routerOp.replacePage(forPath, newLabels));
            }
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

    const widgetHeader = (
        <>
            <CardHeader>
                <CardTitle>
                    <h4>Details & Metadata</h4>
                </CardTitle>
                {!loading && !isEditing && (
                    <CardOptions>
                        <IconButton name="item-edit" text="Edit" onClick={toggleEdit} />
                    </CardOptions>
                )}
            </CardHeader>
            <Divider />
        </>
    );

    const widgetContent = (
        <CardContent>
            {loading && <Loading />}
            {!loading && isEditing && (
                <>
                    <FieldItem
                        key="label"
                        labelAttributes={{
                            text: "Label",
                            htmlFor: "label",
                            info: "required",
                        }}
                        messageText={errors.form.label ? "Label is required" : ""}
                        hasStateDanger={errors.form.label ? true : false}
                    >
                        <Controller
                            as={TextField}
                            name="label"
                            control={control}
                            defaultValue={label}
                            intent={errors.form.label ? Intent.DANGER : Intent.NONE}
                        />
                    </FieldItem>
                    <FieldItem
                        key="description"
                        labelAttributes={{
                            text: "Description",
                            htmlFor: "description",
                        }}
                    >
                        <Controller
                            as={TextArea}
                            name="description"
                            control={control}
                            defaultValue={description}
                            fullWidth={true}
                        />
                    </FieldItem>
                </>
            )}
            {!loading && !isEditing && (
                <>
                    {!!label && <p>Label: {label}</p>}
                    {!!description && <p>Description: {description}</p>}
                </>
            )}
        </CardContent>
    );

    const widgetFooter =
        !loading && isEditing ? (
            <>
                <Divider />
                <CardActions>
                    <Button affirmative text="Save" type={"submit"} />
                    <Button text="Cancel" onClick={toggleEdit} />
                </CardActions>
            </>
        ) : null;

    const widgetFull = (
        <Card>
            {widgetHeader}
            {widgetContent}
            {widgetFooter}
        </Card>
    );

    return isEditing ? <form onSubmit={handleSubmit(onSubmit)}>{widgetFull}</form> : widgetFull;
}
