import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Prompt, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Markdown } from "gui-elements/cmem";
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
    MultiSelect,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    TextArea,
    TextField,
    Link,
} from "gui-elements";
import { Intent } from "gui-elements/blueprint/constants";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";
import { commonSel } from "@ducks/common";
import { routerOp } from "@ducks/router";
import { sharedOp } from "@ducks/shared";
import { Loading } from "../Loading/Loading";
import { StringPreviewContentBlobToggler } from "gui-elements/src/cmem/ContentBlobToggler/StringPreviewContentBlobToggler";
import useErrorHandler from "../../../hooks/useErrorHandler";
import * as H from "history";
import utils from "./MetadataUtils";
import { Tag as TagType } from "./Metadatatypings";

interface IProps {
    projectId?: string;
    taskId?: string;
    readOnly?: boolean;
}

export function Metadata(props: IProps) {
    const location = useLocation();
    const dispatch = useDispatch();
    const { registerError } = useErrorHandler();

    const _projectId = useSelector(commonSel.currentProjectIdSelector);
    const _taskId = useSelector(commonSel.currentTaskIdSelector);

    const projectId = props.projectId || _projectId;
    const taskId = props.taskId || _taskId;

    const [loading, setLoading] = useState(false);
    const [data, setData] = useState({} as IMetadata);
    const [formEditData, setFormEditData] = useState<IMetadataUpdatePayload | undefined>(undefined);
    const [isEditing, setIsEditing] = useState(false);
    const [unsavedChanges, setUnsavedChanges] = useState(false);
    const [tags, setTags] = React.useState<Array<TagType>>([]);
    const [createdTags, setCreatedTags] = React.useState<Array<Partial<TagType>>>([]);
    const [selectedTags, setSelectedTags] = React.useState<Array<TagType>>([...tags]);
    const [t] = useTranslation();

    // Form errors
    const [errors, setErrors] = useState({
        form: {
            label: false,
        },
        alerts: {},
    });

    const setDirtyState = React.useCallback(() => {
        setUnsavedChanges(true);
        window.onbeforeunload = () => true;
    }, []);

    const removeDirtyState = React.useCallback(() => {
        setUnsavedChanges(false);
        window.onbeforeunload = null;
    }, []);

    // On unmount remove dirty state behavior
    React.useEffect(() => {
        return removeDirtyState;
    }, []);

    const { label, description, lastModifiedByUser } = data;

    useEffect(() => {
        if (projectId) {
            getTaskMetadata(taskId, projectId);
            utils.getAllExistingTags(projectId, taskId).then((res) => setTags(res?.data.tags ?? []));
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
            setFormEditData({ label: metaData.label, description: metaData.description });
        } else {
            removeDirtyState();
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

    const onSubmit = async () => {
        if (!formEditData?.label) {
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

        try {
            const result = await letLoading(async () => {
                const path = location.pathname;
                //create new tags if exists
                const createdTagsResponse = await utils.createNewTag(createdTags, projectId);
                //defensive correction to ensure uris match
                const metadataTags = selectedTags.map((tag) => {
                    const newlyCreatedTagMatch = (createdTagsResponse?.data ?? []).find((t) => t.label === tag.label);
                    if (newlyCreatedTagMatch) {
                        return newlyCreatedTagMatch.uri;
                    }
                    return tag.uri;
                });
                formEditData.tags = metadataTags;
                const metadata = await sharedOp.updateTaskMetadataAsync(formEditData!!, taskId, projectId);
                removeDirtyState();
                dispatch(routerOp.updateLocationState(path, projectId as string, metadata));
                return metadata;
            });

            utils.getAllExistingTags(projectId, taskId).then((res) => setTags(res?.data.tags ?? []));
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

    // Show 'unsaved changes' prompt when navigating away via React routing
    const routingPrompt: (newLocation: H.Location, action: H.Action) => string | boolean = (newLocation, action) => {
        // Only complain when navigating away from current page.
        return unsavedChanges && action !== "REPLACE" ? (t("Metadata.unsavedMetaDataWarning") as string) : true;
    };

    const onLabelChange = (e) => {
        if (formEditData && e.target !== undefined) {
            const hasToReRender = !formEditData.label || !e.target.value;
            formEditData.label = e.target.value;
            if (hasToReRender) {
                // Label has changed either from empty or was set to empty. Need to re-render.
                setFormEditData({ ...formEditData });
            }
            checkEditState();
        }
    };

    const onDescriptionChange = (e) => {
        if (formEditData && e.target !== undefined) {
            formEditData.description = e.target.value;
            checkEditState();
        }
    };

    const checkEditState = () => {
        if (formEditData && (formEditData.label !== data.label || formEditData.description !== data.description)) {
            setDirtyState();
        } else {
            removeDirtyState();
        }
    };

    const handleTagSelectionChange = React.useCallback((params) => {
        setCreatedTags(params.createdItems);
        setSelectedTags((oldSelectedTags) => {
            const prevSelectedTags = oldSelectedTags.map((t) => t.uri).join("|");
            const newlySelectedTags = params.selectedItems.map((t) => t.uri).join("|");
            if (prevSelectedTags !== newlySelectedTags) {
                setDirtyState();
            } else {
                removeDirtyState();
            }
            return params.selectedItems;
        });
    }, []);

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
                                <TextField
                                    name="label"
                                    id="label"
                                    onChange={onLabelChange}
                                    defaultValue={formEditData?.label}
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
                                <TextArea
                                    name="description"
                                    id="description"
                                    onChange={onDescriptionChange}
                                    defaultValue={formEditData?.description}
                                    fullWidth={true}
                                />
                            </FieldItem>
                        </PropertyValue>
                    </PropertyValuePair>
                    <PropertyValuePair hasSpacing key="tags">
                        {/** // Todo add german translation for tags here  */}
                        <PropertyName>
                            <Label text={t("form.field.tag", "Tag")} />
                        </PropertyName>
                        <PropertyValue>
                            <FieldItem>
                                <MultiSelect<TagType>
                                    canCreateNewItem
                                    prePopulateWithItems
                                    equalityProp="uri"
                                    labelProp="label"
                                    items={tags}
                                    onSelection={handleTagSelectionChange}
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
                                    fullviewContent={<Markdown>{description}</Markdown>}
                                    toggleExtendText={t("common.words.more", "more")}
                                    toggleReduceText={t("common.words.less", "less")}
                                    firstNonEmptyLineOnly={true}
                                    renderPreviewAsMarkdown={true}
                                    allowedHtmlElementsInPreview={["a"]}
                                />
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!lastModifiedByUser && (
                        <PropertyValuePair hasSpacing hasDivider>
                            {/** // Todo add german translation for author here  */}
                            <PropertyName>{t("form.field.author", "Author")}</PropertyName>
                            <PropertyValue>
                                <Link href="/" onClick={() => {}}>
                                    {lastModifiedByUser}
                                </Link>
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!tags.length && (
                        <PropertyValuePair hasSpacing hasDivider>
                            <PropertyName>{t("form.field.tag", "Tag")}</PropertyName>
                            <PropertyValue>{utils.DisplayArtefactTags(tags, t)}</PropertyValue>
                        </PropertyValuePair>
                    )}
                </PropertyValueList>
            )}
        </CardContent>
    );

    const widgetFooter =
        !loading && isEditing ? (
            <>
                <Prompt when={unsavedChanges} message={routingPrompt} />
                <Divider />
                <CardActions>
                    <Button
                        data-test-id={"submitBtn"}
                        disabled={!unsavedChanges || !formEditData?.label}
                        onClick={onSubmit}
                        affirmative
                        text={t("common.action.save", "Save")}
                        type={"submit"}
                    />
                    <Button text={t("common.action.cancel")} onClick={toggleEdit} />
                </CardActions>
            </>
        ) : null;

    return (
        <Card data-test-id={"meta-data-card"}>
            {widgetHeader}
            {widgetContent}
            {widgetFooter}
        </Card>
    );
}
