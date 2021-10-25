import React, { useEffect, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useDispatch, useSelector } from "react-redux";
import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Markdown } from "@gui-elements/cmem";
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
    Label,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    TextArea,
    TextField,
} from "@gui-elements/index";
import { Intent } from "@gui-elements/blueprint/constants";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { sharedOp } from "@ducks/shared";
import { Loading } from "../Loading/Loading";
import { StringPreviewContentBlobToggler } from "@gui-elements/src/cmem/ContentBlobToggler/StringPreviewContentBlobToggler";
import useErrorHandler from "../../../hooks/useErrorHandler";

interface IProps {
    projectId?: string;
    taskId?: string;
    readOnly?: boolean;
}

export function Metadata(props: IProps) {
    const { control, handleSubmit } = useForm();
    const location = useLocation();
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();

    const _projectId = useSelector(commonSel.currentProjectIdSelector);
    const _taskId = useSelector(commonSel.currentTaskIdSelector);

    const projectId = props.projectId || _projectId;
    const taskId = props.taskId || _taskId;

    const [loading, setLoading] = useState(false);
    const [data, setData] = useState({} as IMetadata);
    const [editData, setEditData] = useState({} as IMetadata);
    const [isEditing, setIsEditing] = useState(false);
    const [t] = useTranslation();

    // Form errors
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

    const getTaskMetadata = async (taskId?: string, projectId?: string) => {
        try {
            const result = await letLoading(() => {
                return sharedOp.getTaskMetadataAsync(taskId, projectId);
            });
            setData(result);
            return result;
        } catch (error) {
            registerError("Metadata-getTaskMetaData", "Fetching meta data has failed.", error);
            return {};
        }
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
        // Store if error occurs
        setEditData(inputs);

        try {
            const result = await letLoading(async () => {
                const path = location.pathname;
                const metadata = await sharedOp.updateTaskMetadataAsync(inputs, taskId, projectId);
                dispatch(routerOp.updateLocationState(path, projectId as string, metadata));
                return metadata;
            });

            setData(result);

            toggleEdit();
        } catch (ex) {
            registerError("Metadata-submit", "Updating meta data has failed.", ex);
        }
    };

    const widgetHeader = (
        <>
            <CardHeader>
                <CardTitle>
                    <h2>{t("common.words.summary", "Summary")}</h2>
                </CardTitle>
                {!loading && !isEditing && !props.readOnly && (
                    <CardOptions>
                        <IconButton
                            data-test-id="meta-data-edit-btn"
                            name="item-edit"
                            text="Edit"
                            onClick={toggleEdit}
                        />
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
                                <StringPreviewContentBlobToggler
                                    className="di__dataset__metadata-description"
                                    content={description}
                                    previewMaxLength={128}
                                    fullviewContent={<Markdown>{description}</Markdown>}
                                    toggleExtendText={t("common.words.more", "more")}
                                    toggleReduceText={t("common.words.less", "less")}
                                    firstNonEmptyLineOnly={true}
                                />
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
                    <Button
                        data-test-id={"submitBtn"}
                        affirmative
                        text={t("common.action.submit", "Submit")}
                        type={"submit"}
                    />
                    <Button text={t("common.action.cancel")} onClick={toggleEdit} />
                </CardActions>
            </>
        ) : null;

    const widgetFull = (
        <Card data-test-id={"meta-data-card"}>
            {widgetHeader}
            {widgetContent}
            {widgetFooter}
        </Card>
    );

    return isEditing ? <form onSubmit={handleSubmit(onSubmit)}>{widgetFull}</form> : widgetFull;
}
