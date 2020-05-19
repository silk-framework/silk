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
import { Loading } from "../Loading/Loading";
import { Controller, useForm } from "react-hook-form";
import { Intent } from "@wrappers/blueprint/constants";
import { useLocation } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";

interface IProps {
    projectId?: string;
    taskId?: string;
}

export function Metadata(props: IProps) {
    const { control, handleSubmit } = useForm();
    const location = useLocation();
    const dispatch = useDispatch();

    const _projectId = useSelector(commonSel.currentProjectIdSelector);
    const _taskId = useSelector(commonSel.currentTaskIdSelector);

    const projectId = props.projectId || _projectId;
    const taskId = props.taskId || _taskId;

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

        const result = await letLoading(async () => {
            const path = location.pathname;
            const metadata = await sharedOp.updateTaskMetadataAsync(taskId, inputs, projectId);
            dispatch(routerOp.updateLocationState(path, projectId, metadata));
            return metadata;
        });

        setData(result);

        toggleEdit();
    };

    const widgetHeader = (
        <>
            <CardHeader>
                <CardTitle>
                    <h2>Summary</h2>
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
            {loading && <Loading description="Loading summary data." />}
            {!loading && isEditing && (
                <PropertyValueList>
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
