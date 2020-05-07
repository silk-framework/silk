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
    HtmlContentBlock,
    IconButton,
    PropertyValueList,
    PropertyValuePair,
    PropertyName,
    PropertyValue,
    Label,
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
                <PropertyValueList>
                    {!!label && (
                        <PropertyValuePair key="label">
                            <PropertyName>
                                <Label text="Label" info="required" htmlFor="label" />
                            </PropertyName>
                            <PropertyValue>
                                <FieldItem
                                    messageText={errors.form.label ? "Label is required" : ""}
                                    hasStateDanger={errors.form.label ? true : false}
                                >
                                    <Controller
                                        as={TextField}
                                        name="label"
                                        id="label"
                                        control={control}
                                        defaultValue={label}
                                        intent={errors.form.label ? Intent.DANGER : Intent.NONE}
                                    />
                                </FieldItem>
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!description && (
                        <PropertyValuePair hasSpacing key="description">
                            <PropertyName>
                                <Label text="Description" htmlFor="description" />
                            </PropertyName>
                            <PropertyValue>
                                <FieldItem>
                                    <Controller
                                        as={TextArea}
                                        name="description"
                                        id="description"
                                        control={control}
                                        defaultValue={description}
                                        fullWidth={true}
                                    />
                                </FieldItem>
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                </PropertyValueList>
            )}
            {!loading && !isEditing && (
                <PropertyValueList>
                    {!!label && (
                        <PropertyValuePair hasDivider>
                            <PropertyName>Label</PropertyName>
                            <PropertyValue>{label}</PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!description && (
                        <PropertyValuePair hasSpacing hasDivider>
                            <PropertyName>Description</PropertyName>
                            <PropertyValue>
                                <HtmlContentBlock>
                                    <p>{description}</p>
                                </HtmlContentBlock>
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                </PropertyValueList>
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
        ) : (
            <></>
        );

    const widgetFull = (
        <Card>
            {widgetHeader}
            {widgetContent}
            {widgetFooter}
        </Card>
    );

    return isEditing ? <form onSubmit={handleSubmit(onSubmit)}>{widgetFull}</form> : widgetFull;
}
