import React, { KeyboardEventHandler, useEffect, useState } from "react";
import {
    Button,
    FieldItem,
    IconButton,
    Notification,
    SimpleDialog,
    Spacing,
    Spinner,
    TextField,
} from "@eccenca/gui-elements";
import { ErrorResponse, FetchError } from "../../../services/fetch/responseInterceptor";
import {
    fetchProjectAccessControl,
    fetchUserData,
    requestCloneProject,
    requestCloneTask,
} from "@ducks/workspace/requests";
import { requestProjectMetadata, requestTaskMetadata } from "@ducks/shared/requests";
import { useTranslation } from "react-i18next";
import { IModalItem } from "@ducks/shared/typings";
import useHotKey from "../HotKeyHandler/HotKeyHandler";
import { requestProjectIdValidation, requestTaskIdValidation } from "@ducks/common/requests";
import { debounce } from "lodash";
import { TaskDocumentationModal } from "./CreateArtefactModal/TaskDocumentationModal";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { pluginRegistry, SUPPORTED_PLUGINS } from "../../plugins/PluginRegistry";
import { ProjectAccessControlManagementProps } from "../../plugins/plugin.types";

export interface ICloneOptions {
    item: IModalItem;

    onDiscard(): void;

    onConfirmed?(newLabel: string, detailsPage: string): void;
}

export default function CloneModal({ item, onDiscard, onConfirmed }: ICloneOptions) {
    // Value of the new label for the cloned project or task
    const [newLabel, setNewLabel] = useState(item.label || item.id || item.projectLabel || item.projectId);
    const [description, setDescription] = useState(item.description);
    const [customId, setCustomId] = React.useState<string>("");
    const [identifierValidationMsg, setIdentifierValidationMsg] = React.useState<string>("");
    const [initialLoading, setInitialLoading] = useState(true);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<ErrorResponse | null>(null);
    // Label of the project or task that should be cloned
    const [label, setLabel] = useState<string | undefined>(item.label);
    const [t] = useTranslation();
    const [showDocumentation, setShowDocumentation] = React.useState<boolean>(false);
    const initialSettings = useSelector(commonSel.initialSettingsSelector);
    const aclEnabled = initialSettings?.aclEnabled ?? false;
    const [initialAclGroups, setInitialAclGroups] = useState<string[] | undefined>(undefined);
    const [aclGroups, setAclGroups] = useState<string[]>([]);
    const projectAclManagement = pluginRegistry.pluginReactComponent<ProjectAccessControlManagementProps>(
        SUPPORTED_PLUGINS.DI_PROJECT_ACL_MANAGEMENT,
    );

    useEffect(() => {
        prepareCloning();
    }, [item]);

    const prepareCloning = async () => {
        setInitialLoading(true);
        try {
            const response =
                item.projectId && item.id
                    ? await requestTaskMetadata(item.id, item.projectId)
                    : await requestProjectMetadata(item.projectId);
            const currentLabel = !!response.data.label ? response.data.label : !!item.id ? item.id : item.projectId;
            setLabel(currentLabel);
            setDescription(response.data.description);
            setNewLabel(t("common.messages.cloneOf", { item: currentLabel }));
            if (!item.id && aclEnabled) {
                try {
                    const [aclRes, userRes] = await Promise.all([
                        fetchProjectAccessControl(item.projectId),
                        fetchUserData(),
                    ]);
                    const intersection = userRes.data.groups.filter((g) => aclRes.data.groups.includes(g));
                    setInitialAclGroups(intersection);
                    setAclGroups(intersection);
                } catch {
                    // swallow exception, ACL widget will fall back to fetching from project
                }
            }
        } catch (ex) {
            // swallow exception, fallback to ID
        } finally {
            setInitialLoading(false);
        }
    };

    const verifyCustomId = debounce(
        React.useCallback(
            async (id: string) => {
                if (id) {
                    try {
                        const res = !item.id
                            ? await requestProjectIdValidation(id)
                            : await requestTaskIdValidation(id, item.projectId);

                        if (res.axiosResponse.status === 204) {
                            setIdentifierValidationMsg("");
                        }
                    } catch (err) {
                        if (err.httpStatus === 409) {
                            setIdentifierValidationMsg(t("CreateModal.CustomIdentifierInput.validations.unique"));
                        } else if (err.httpStatus === 400) {
                            setIdentifierValidationMsg(t("CreateModal.CustomIdentifierInput.validations.invalid"));
                        } else {
                            setIdentifierValidationMsg("There has been an error validating the custom ID.");
                        }
                    }
                }
            },
            [item],
        ),
        1000,
    );

    const handleCustomIdChange = React.useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            const newCustomId = e.target.value.trim();
            setCustomId(newCustomId);
            verifyCustomId(newCustomId);
        },
        [item],
    );

    const handleCloning = async () => {
        const { projectId, id } = item;
        setError(null);
        try {
            setLoading(true);
            const payload = {
                metaData: {
                    label: newLabel,
                    description,
                },
            };

            const response = id
                ? await requestCloneTask(id, projectId, payload, customId)
                : await requestCloneProject(projectId, { ...payload, newTaskId: customId, groups: aclGroups });
            onConfirmed && onConfirmed(newLabel, response.data.detailsPage);
        } catch (e) {
            if (e.isFetchError) {
                setError((e as FetchError).errorResponse);
            } else {
                console.warn(e);
            }
        } finally {
            setLoading(false);
        }
    };

    useHotKey({ hotkey: "enter", handler: handleCloning });
    const enterHandler: KeyboardEventHandler<HTMLInputElement> = React.useCallback(
        (event): void => {
            if (event.key === "Enter") {
                handleCloning();
            }
        },
        [item, description, newLabel, customId],
    );

    return (
        <SimpleDialog
            data-test-id={"clone-item-to-modal"}
            size="small"
            title={
                t("common.action.CloneSmth", {
                    smth: t(item.id ? "common.dataTypes.task" : "common.dataTypes.project"),
                }) +
                    ": " +
                    label ||
                item.label ||
                item.id
            }
            isOpen={true}
            preventSimpleClosing={loading}
            onClose={onDiscard}
            headerOptions={
                <IconButton
                    key={"show-enhanced-description-btn"}
                    name="item-question"
                    onClick={() => setShowDocumentation(true)}
                />
            }
            actions={[
                <Button
                    key="clone"
                    affirmative
                    onClick={handleCloning}
                    disabled={!newLabel || initialLoading}
                    loading={loading}
                    data-test-id={"clone-modal-button"}
                >
                    {t("common.action.clone")}
                </Button>,
                <Button key="cancel" onClick={onDiscard} disabled={loading}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <FieldItem
                labelProps={{
                    htmlFor: "label",
                    text: t("common.messages.cloneModalTitle", {
                        item: item.id ? t("common.dataTypes.task") : t("common.dataTypes.project"),
                    }),
                }}
            >
                {initialLoading ? (
                    <Spinner position={"inline"} size={"small"} />
                ) : (
                    <TextField onChange={(e) => setNewLabel(e.target.value)} value={newLabel} autoFocus={true} />
                )}
            </FieldItem>

            <FieldItem
                labelProps={{
                    htmlFor: "customId",
                    text: t("common.messages.cloneModalIdentifier", {
                        item: item.id ? t("common.dataTypes.task") : t("common.dataTypes.project"),
                    }),
                }}
                intent={!!identifierValidationMsg ? "danger" : undefined}
                messageText={identifierValidationMsg}
            >
                <TextField
                    data-test-id="clone-custom-id"
                    intent={!!identifierValidationMsg ? "danger" : "none"}
                    onChange={handleCustomIdChange}
                    value={customId}
                    autoFocus={true}
                    onKeyUp={enterHandler}
                />
            </FieldItem>
            {!item.id && aclEnabled && !initialLoading && projectAclManagement && (
                <>
                    <Spacing />
                    <projectAclManagement.Component
                        projectId={item.projectId}
                        initialGroups={initialAclGroups}
                        onChange={setAclGroups}
                    />
                </>
            )}
            {error && (
                <>
                    <Spacing />
                    <Notification message={error.asString()} intent="danger" />
                </>
            )}
            {showDocumentation && (
                <TaskDocumentationModal
                    documentationToShow={{ key: "", namedAnchor: "", description: t("cloneModal.info") }}
                    onClose={() => setShowDocumentation(false)}
                    size="tiny"
                />
            )}
        </SimpleDialog>
    );
}
