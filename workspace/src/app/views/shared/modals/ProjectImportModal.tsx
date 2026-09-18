import React, { useEffect, useState } from "react";
import {
    Button,
    CardActionsAux,
    Checkbox,
    FileUpload,
    FileUploadHandle,
    FileUploadResponseMetadata,
    Notification,
    PropertyName,
    PropertyValue,
    PropertyValueList,
    PropertyValuePair,
    SimpleDialog,
    Spacing,
    TitleSubsection,
    Markdown,
    StringPreviewContentBlobToggler,
} from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import {
    AccessControlConfig,
    requestDeleteProjectImport,
    requestProjectImportDetails,
    requestProjectImportExecutionStatus,
    requestStartProjectImport,
} from "@ducks/workspace/requests";
import { IProjectExecutionStatus, IProjectImportDetails } from "@ducks/workspace/typings";
import { Loading } from "../Loading/Loading";
import { useDispatch } from "react-redux";
import { routerOp } from "@ducks/router";
import { absoluteProjectPath } from "../../../utils/routerUtils";
import { useFileUploadLabels } from "../FileUploader/useFileUploadLabels";
import { useProjectAclManagementComponent } from "../../../hooks/useProjectAclManagementComponent";
import { AppDispatch } from "store/configureStore";

interface IProps {
    // Called when closing the modal
    close: () => void;
    // Optional back action
    back?: () => void;
    /** The max. file upload size in bytes. */
    maxFileUploadSizeBytes?: number;
}

const parseProjectImportId = ({ responseText }: FileUploadResponseMetadata): string => {
    const body: unknown = JSON.parse(responseText);
    if (
        typeof body !== "object" ||
        body === null ||
        !("projectImportId" in body) ||
        typeof body.projectImportId !== "string" ||
        !body.projectImportId.trim()
    ) {
        throw new Error("Invalid project import response");
    }
    return body.projectImportId;
};

const errorDetails = (error: unknown): string => {
    const message =
        typeof error === "object" && error !== null && "message" in error && typeof error.message === "string"
            ? error.message
            : "";
    return message.split("Source error")[0].trim();
};

export function ProjectImportModal({ close, back, maxFileUploadSizeBytes }: IProps) {
    const [t] = useTranslation();
    const uploaderRef = React.useRef<FileUploadHandle<string>>(null);
    const uploadLabels = useFileUploadLabels();
    const dispatch = useDispatch<AppDispatch>();
    const [loading, setLoading] = useState(false);
    const [projectImportId, setProjectImportId] = useState<string | null>(null);
    const [projectImportDetails, setProjectImportDetails] = useState<IProjectImportDetails | null>(null);
    const [approveReplacement, setApproveReplacement] = useState(false);
    // Unexpected error for the project details request
    const [projectDetailsError, setProjectDetailsError] = useState<string | null>(null);
    // Unexpected error for the project import execution request
    const [startProjectImportExecutionError, setStartProjectImportExecutionError] = useState<
        [string, boolean, boolean] | null
    >(null);
    const projectAcl = React.useRef<AccessControlConfig | undefined>();
    const onChangeProjectAcl = React.useCallback((newProjectAcl: AccessControlConfig) => {
        projectAcl.current = newProjectAcl;
    }, []);
    const isUnmounted = React.useRef(false);
    const importCancelled = React.useRef(false);
    const pendingSleepTimeoutId = React.useRef<number | null>(null);
    const resolvePendingSleep = React.useRef<(() => void) | undefined>();
    const aclManagement = useProjectAclManagementComponent({
        onChange: onChangeProjectAcl,
        externalInitialAclGroups: { groups: [] },
    });

    const clearPendingImportTimeout = React.useCallback(() => {
        if (pendingSleepTimeoutId.current != null) {
            clearTimeout(pendingSleepTimeoutId.current);
            pendingSleepTimeoutId.current = null;
        }
        resolvePendingSleep.current?.();
        resolvePendingSleep.current = undefined;
    }, []);

    const stopPendingImport = React.useCallback(() => {
        importCancelled.current = true;
        clearPendingImportTimeout();
    }, [clearPendingImportTimeout]);

    const setLoadingIfMounted = React.useCallback((nextLoading: boolean) => {
        if (!isUnmounted.current && !importCancelled.current) {
            setLoading(nextLoading);
        }
    }, []);

    useEffect(() => {
        isUnmounted.current = false;
        importCancelled.current = false;
        return () => {
            isUnmounted.current = true;
            stopPendingImport();
        };
    }, [stopPendingImport]);

    useEffect(() => {
        if (projectImportId) {
            loadProjectImportDetails(projectImportId);
        }
    }, [projectImportId]);

    const loadProjectImportDetails = async (projectImportId: string) => {
        if (isUnmounted.current || importCancelled.current) {
            return;
        }
        setProjectDetailsError(null);
        try {
            setLoadingIfMounted(true);
            const response = await requestProjectImportDetails(projectImportId);
            if (!isUnmounted.current && !importCancelled.current) {
                setProjectImportDetails(response.data);
            }
        } catch (ex) {
            if (!isUnmounted.current && !importCancelled.current) {
                setProjectDetailsError(errorDetails(ex));
            }
        } finally {
            setLoadingIfMounted(false);
        }
    };

    const closeDialog = async () => {
        if (importCancelled.current) return;
        stopPendingImport();
        uploaderRef.current?.cancel();
        setLoading(true);
        await cleanUp();
        close();
    };

    const goBack = async () => {
        if (importCancelled.current) return;
        stopPendingImport();
        uploaderRef.current?.cancel();
        setLoading(true);
        await cleanUp();
        back?.();
    };

    // Deletes the uploaded file in the backend
    const cleanUp = async () => {
        if (projectImportId) {
            try {
                await requestDeleteProjectImport(projectImportId);
            } catch (ex) {
                // If this fails for whatever reason the backend will remove the file automatically after a specific period
            }
        }
    };

    const startProjectImport = async (generateNewProjectId: boolean, overWriteExistingProject: boolean) => {
        if (loading || importCancelled.current) return;
        setStartProjectImportExecutionError(null);
        if (projectImportId) {
            try {
                setLoadingIfMounted(true);
                await requestStartProjectImport(
                    projectImportId,
                    generateNewProjectId,
                    overWriteExistingProject,
                    overWriteExistingProject ? undefined : projectAcl.current?.groups,
                );
                let status: Partial<IProjectExecutionStatus> = {};
                const sleep = (ms: number) =>
                    new Promise<void>((resolve) => {
                        resolvePendingSleep.current = resolve;
                        pendingSleepTimeoutId.current = window.setTimeout(() => {
                            pendingSleepTimeoutId.current = null;
                            resolvePendingSleep.current = undefined;
                            resolve();
                        }, ms);
                    });
                let errorCounter = 0;
                while (!status.importEnded && !importCancelled.current) {
                    try {
                        status = (await requestProjectImportExecutionStatus(projectImportId)).data;
                        errorCounter = 0;
                    } catch (err) {
                        if (importCancelled.current || isUnmounted.current) return;
                        if (errorCounter >= 6) {
                            throw err;
                        }
                        // Retry until error persists for overall 120 seconds, exponential backoff
                        await sleep(Math.pow(2, errorCounter) * 1000);
                        if (importCancelled.current) {
                            return;
                        }
                        errorCounter = errorCounter + 1;
                    }
                }
                if (importCancelled.current || isUnmounted.current) {
                    return;
                }
                if (status.success) {
                    close();
                    dispatch(routerOp.goToPage(absoluteProjectPath(status.projectId!)));
                } else {
                    setStartProjectImportExecutionError([
                        status.failureMessage ?? t("ProjectImportModal.importFailed"),
                        generateNewProjectId,
                        overWriteExistingProject,
                    ]);
                }
            } catch (ex) {
                if (!importCancelled.current && !isUnmounted.current) {
                    setStartProjectImportExecutionError([
                        errorDetails(ex),
                        generateNewProjectId,
                        overWriteExistingProject,
                    ]);
                }
            } finally {
                clearPendingImportTimeout();
                setLoadingIfMounted(false);
            }
        }
    };

    const handleApproveReplacement = () => {
        setApproveReplacement(!approveReplacement);
    };

    const uploader = (
        <FileUpload
            ref={uploaderRef}
            name={t("ProjectImportModal.projectFile")}
            endpoint={workspaceApi("/projectImport")}
            method="POST"
            maxNumberOfFiles={1}
            maxFileSize={maxFileUploadSizeBytes}
            selectionDisabled={projectImportId !== null}
            parseResponse={parseProjectImportId}
            onUploadSuccess={({ body }) => {
                if (!isUnmounted.current && !importCancelled.current) setProjectImportId(body);
            }}
            labels={{
                ...uploadLabels,
                formatError: (error) => {
                    if (error.kind === "response") return t("ProjectImportModal.responseInvalid");
                    if (error.kind === "transport")
                        return t("ProjectImportModal.responseUploadError", {
                            file: error.file?.name ?? "",
                            details: errorDetails(error.error),
                        });
                    return uploadLabels.formatError(error);
                },
            }}
        />
    );
    const actions: React.JSX.Element[] = [];
    if (projectImportDetails) {
        if (!projectImportDetails.errorMessage && !projectImportDetails.projectAlreadyExists) {
            actions.push(
                <Button
                    data-test-id={"startImportProjectBtn"}
                    key="importProject"
                    affirmative={true}
                    disabled={loading}
                    onClick={() => startProjectImport(false, false)}
                >
                    {t("ProjectImportModal.importBtn")}
                </Button>,
            );
        } else if (projectImportDetails.projectAlreadyExists) {
            approveReplacement
                ? actions.push(
                      <Button
                          data-test-id={"replaceImportProjectBtn"}
                          key="replaceProject"
                          disruptive={true}
                          disabled={loading}
                          onClick={() => startProjectImport(false, true)}
                      >
                          {t("ProjectImportModal.replaceImportBtn")}
                      </Button>,
                  )
                : actions.push(
                      <Button
                          data-test-id={"importUnderFreshIdBtn"}
                          key="importAsFreshProject"
                          affirmative={true}
                          disabled={loading}
                          onClick={() => startProjectImport(true, false)}
                      >
                          {t("ProjectImportModal.importUnderFreshIdBtn")}
                      </Button>,
                  );
        }
    }
    // Add 'Cancel' button
    actions.push(
        <Button key="cancel" onClick={closeDialog}>
            {t("common.action.cancel")}
        </Button>,
    );
    // Add 'Back' button
    actions.push(
        <CardActionsAux key="aux">
            {back && (
                <Button key="back" onClick={goBack}>
                    {t("common.words.back")}
                </Button>
            )}
        </CardActionsAux>,
    );

    const projectDetails = (details: IProjectImportDetails) => {
        return (
            <>
                <TitleSubsection>{t("ProjectImportModal.importSummary")}</TitleSubsection>
                <PropertyValueList>
                    {!!details.label && (
                        <PropertyValuePair hasDivider key={"label"}>
                            <PropertyName>{t("form.field.label")}</PropertyName>
                            <PropertyValue>{details.label}</PropertyValue>
                        </PropertyValuePair>
                    )}
                    {!!details.description && (
                        <PropertyValuePair hasSpacing hasDivider>
                            <PropertyName>{t("form.field.description")}</PropertyName>
                            <PropertyValue>
                                <StringPreviewContentBlobToggler
                                    className="di__dataset__metadata-description"
                                    content={details.description}
                                    previewMaxLength={128}
                                    fullviewContent={
                                        <Markdown htmlContentBlockProps={{ linebreakForced: true }}>
                                            {details.description}
                                        </Markdown>
                                    }
                                    toggleExtendText={t("common.words.more")}
                                    toggleReduceText={t("common.words.less")}
                                    useOnly={"firstNonEmptyLine"}
                                />
                            </PropertyValue>
                        </PropertyValuePair>
                    )}
                </PropertyValueList>
                {!approveReplacement && aclManagement.component ? aclManagement.component : null}
            </>
        );
    };

    const projectExistsNotification = (details: IProjectImportDetails) => {
        const cannotOverwrite = details.noAccess;
        return (
            <Notification
                intent="warning"
                actions={
                    cannotOverwrite
                        ? undefined
                        : [
                              <Button
                                  key={"openExistingProjectKey"}
                                  href={absoluteProjectPath(details.projectId)}
                                  target={"_empty"}
                              >
                                  {t("ProjectImportModal.openExistingProject")}
                              </Button>,
                          ]
                }
            >
                <Markdown>
                    {t(
                        cannotOverwrite
                            ? "ProjectImportModal.warningExistingProjectForbidden"
                            : "ProjectImportModal.warningExistingProject",
                    )}
                </Markdown>
                <Spacing />
                <Checkbox
                    data-test-id={"replaceExistingProjectCheckBox"}
                    inline={true}
                    checked={approveReplacement}
                    onChange={handleApproveReplacement}
                >
                    <strong>{t("ProjectImportModal.replaceImportBtn")}</strong>
                </Checkbox>
            </Notification>
        );
    };

    const projectDetailElement = (details: IProjectImportDetails) => {
        if (details.projectAlreadyExists) {
            return (
                <>
                    {projectExistsNotification(details)}
                    <Spacing />
                    {projectDetails(details)}
                </>
            );
        } else if (details.errorMessage) {
            return (
                <Notification
                    intent="danger"
                    message={t("ProjectImportModal.analysisError", { details: details.errorMessage })}
                />
            );
        } else {
            return projectDetails(details);
        }
    };

    const errorRetryElement = (errorMessage: string, retryAction: () => void) => {
        return (
            <>
                <Notification intent="danger" message={errorMessage} />
                <Spacing />
                <Button
                    data-test-id={"retryProjectDetailsBtn"}
                    affirmative={true}
                    onClick={retryAction}
                    disabled={false}
                >
                    {t("common.action.retry")}
                </Button>
            </>
        );
    };

    const dialogItem = loading ? (
        <Loading delay={0} />
    ) : projectDetailsError !== null ? (
        errorRetryElement(
            t("ProjectImportModal.detailsLoadError", { details: projectDetailsError }),
            () => projectImportId && loadProjectImportDetails(projectImportId),
        )
    ) : startProjectImportExecutionError ? (
        errorRetryElement(`${t("common.messages.anErrorHasOccurred")} ${startProjectImportExecutionError[0]}`, () =>
            startProjectImport(startProjectImportExecutionError[1], startProjectImportExecutionError[2]),
        )
    ) : projectImportDetails ? (
        projectDetailElement(projectImportDetails)
    ) : (
        uploader
    );

    return (
        <SimpleDialog
            preventSimpleClosing={true}
            hasBorder
            title={t("ProjectImportModal.title")}
            isOpen={true}
            actions={actions}
        >
            {dialogItem}
        </SimpleDialog>
    );
}
