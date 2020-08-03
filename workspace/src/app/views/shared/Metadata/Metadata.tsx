import React, { useEffect, useState } from "react";
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
    Notification,
    PropertyValueList,
    PropertyValuePair,
    PropertyName,
    PropertyValue,
    Label,
    TextArea,
    TextField,
    Spacing,
} from "@wrappers/index";
import { Controller, useForm } from "react-hook-form";
import { Intent } from "@wrappers/blueprint/constants";
import { useDispatch, useSelector } from "react-redux";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import { routerOp } from "@ducks/router";
import { sharedOp } from "@ducks/shared";
import { Loading } from "../Loading/Loading";
import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";

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
    const [editData, setEditData] = useState({} as IMetadata);
    const [isEditing, setIsEditing] = useState(false);
    const [getRequestError, setGetRequestError] = useState<ErrorResponse | null>(null);
    const [updateRequestError, setUpdateRequestError] = useState<ErrorResponse | null>(null);
    const [t] = useTranslation();

    const [errors, setErrors] = useState({
        form: {
            label: false,
        },
        alerts: {},
    });

    const { label, description } = data;

    useEffect(() => {
        if (projectId) {
            getTaskMetadata(taskId, projectId);
        }
    }, [taskId, projectId]);

    const letLoading = async (callback) => {
        setLoading(true);
        try {
            return await callback();
        } finally {
            setLoading(false);
        }
    };

    const toggleEdit = async () => {
        if (!isEditing) {
            let metaData = data;
            if (!metaData.label) {
                metaData = await getTaskMetadata(taskId, projectId);
                if (!metaData.label) {
                    return; // Do not toggle edit mode, request has failed
                }
            }
            setEditData(metaData);
        }
        setIsEditing(!isEditing);
    };

    const getTaskMetadata = async (taskId: string, projectId: string) => {
        setGetRequestError(null);
        try {
            const result = await letLoading(() => {
                return sharedOp.getTaskMetadataAsync(taskId, projectId);
            });
            setData(result);
            return result;
        } catch (error) {
            if (error.isFetchError) {
                setGetRequestError((error as FetchError).errorResponse);
            }
            return {};
        }
    };

    const onSubmit = async (inputs: IMetadataUpdatePayload) => {
        setUpdateRequestError(null);
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
        // Store if error occurs
        setEditData(inputs);

        try {
            const result = await letLoading(async () => {
                const path = location.pathname;
                const metadata = await sharedOp.updateTaskMetadataAsync(inputs, taskId, projectId);
                dispatch(routerOp.updateLocationState(path, projectId, metadata));
                return metadata;
            });

            setData(result);

            toggleEdit();
        } catch (ex) {
            if (ex.isFetchError) {
                if (ex.isHttpError) {
                    setUpdateRequestError(
                        new ErrorResponse(
                            t("Metadata.updateFailed", "Updating meta data has failed"),
                            ex.errorResponse.detail
                        )
                    );
                } else {
                    setUpdateRequestError(ex.errorResponse);
                }
            } else {
                console.warn("Meta data update request has failed: " + ex);
            }
        }
    };

    const widgetHeader = (
        <>
            <CardHeader>
                <CardTitle>
                    <h2>{t("common.words.summary", "Summary")}</h2>
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
        <CardContent data-test-id={"metaDataWidget"}>
            {loading && <Loading description={t("Metadata.loading", "Loading summary data.")} />}
            {!loading && isEditing && (
                <PropertyValueList>
                    <PropertyValuePair key="label">
                        <PropertyName>
                            <Label
                                text={t("form.field.label", "Label")}
                                info={t("common.words.required")}
                                htmlFor="label"
                            />
                        </PropertyName>
                        <PropertyValue>
                            <FieldItem
                                messageText={
                                    errors.form.label ? t("form.validations.isRequired", { field: `Label` }) : ""
                                }
                                hasStateDanger={errors.form.label}
                            >
                                <Controller
                                    as={TextField}
                                    name="label"
                                    id="label"
                                    control={control}
                                    defaultValue={editData.label}
                                    intent={errors.form.label ? Intent.DANGER : Intent.NONE}
                                />
                            </FieldItem>
                        </PropertyValue>
                    </PropertyValuePair>
                    <PropertyValuePair hasSpacing key="description">
                        <PropertyName>
                            <Label text={t("form.field.description", "Description")} htmlFor="description" />
                        </PropertyName>
                        <PropertyValue>
                            <FieldItem>
                                <Controller
                                    as={TextArea}
                                    name="description"
                                    id="description"
                                    control={control}
                                    defaultValue={editData.description}
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
                            <PropertyName>{t("form.field.label", "Label")}</PropertyName>
                            <PropertyValue>{label}</PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!description && (
                        <PropertyValuePair hasSpacing hasDivider>
                            <PropertyName>{t("form.field.description", "Description")}</PropertyName>
                            <PropertyValue>
                                <HtmlContentBlock>
                                    <p>{description}</p>
                                </HtmlContentBlock>
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                </PropertyValueList>
            )}
            {getRequestError && (
                <>
                    <Spacing />
                    <Notification message={getRequestError.asString()} danger />
                </>
            )}
            {updateRequestError && (
                <>
                    <Spacing />
                    <Notification message={updateRequestError.asString()} danger />
                </>
            )}
        </CardContent>
    );

    const widgetFooter =
        !loading && isEditing ? (
            <>
                <Divider />
                <CardActions>
                    <Button affirmative text={t("common.action.submit", "Submit")} type={"submit"} />
                    <Button text={t("common.action.cancel")} onClick={toggleEdit} />
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
