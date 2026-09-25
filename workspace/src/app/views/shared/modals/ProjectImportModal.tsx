import React, { useEffect, useState } from "react";
import {
    Button,
    CardActionsAux,
    Checkbox,
    FieldItem,
    RadioButton,
    TextField,
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
import Uppy, { UppyFile } from "@uppy/core";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import XHR from "@uppy/xhr-upload";
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
import { UploadNewFile } from "../FileUploader/cases/UploadNewFile/UploadNewFile";
import { useProjectAclManagementComponent } from "../../../hooks/useProjectAclManagementComponent";
import { requestProjectIdValidation } from "@ducks/common/requests";
import { AppDispatch } from "store/configureStore";

interface IProps {
    // Called when closing the modal
    close: () => void;
    // Optional back action
    back?: () => void;
    /** The max. file upload size in bytes. */
    maxFileUploadSizeBytes?: number;
}

type ImportDestination = "original" | "generated" | "custom";

export function ProjectImportModal({ close, back, maxFileUploadSizeBytes }: IProps) {
    const [t] = useTranslation();
    const [uppy] = useState(() => Uppy());
    const dispatch = useDispatch<AppDispatch>();
    const [loading, setLoading] = useState(false);
    const [projectImportId, setProjectImportId] = useState<string | null>(null);
    const [projectImportDetails, setProjectImportDetails] = useState<IProjectImportDetails | null>(null);
    const [approveReplacement, setApproveReplacement] = useState(false);
    const [destination, setDestination] = useState<ImportDestination>("original");
    const [customProjectId, setCustomProjectId] = useState("");
    const [validatedProjectId, setValidatedProjectId] = useState<string | null>(null);
    const [customIdError, setCustomIdError] = useState<string | null>(null);
    const replacingProject = destination === "original" && !!projectImportDetails?.projectAlreadyExists;
    const changeDestination = (value: ImportDestination) => {
        setDestination(value);
        setApproveReplacement(false);
        setStartProjectImportExecutionError(null);
    };
    useEffect(() => {
        setValidatedProjectId(null);
        setCustomIdError(null);
        if (destination !== "custom" || !customProjectId) return;
        let cancelled = false;
        const timeout = window.setTimeout(async () => {
            try {
                await requestProjectIdValidation(customProjectId);
                if (!cancelled) setValidatedProjectId(customProjectId);
            } catch (error) {
                if (!cancelled) {
                    setCustomIdError(
                        t(
                            error.httpStatus === 409
                                ? "ProjectImportModal.projectIdAlreadyExists"
                                : error.httpStatus === 400
                                  ? "CreateModal.CustomIdentifierInput.validations.invalid"
                                  : "ProjectImportModal.validationFailed",
                        ),
                    );
                }
            }
        }, 200);
        return () => {
            cancelled = true;
            window.clearTimeout(timeout);
        };
    }, [destination, customProjectId, t]);
    // Unexpected error for the file upload request
    const [uploadError, setUploadError] = useState<string | null>(null);
    // Unexpected error for the project details request
    const [projectDetailsError, setProjectDetailsError] = useState<string | null>(null);
    // Unexpected error for the project import execution request
    const [startProjectImportExecutionError, setStartProjectImportExecutionError] = useState<string | null>(null);
    const projectAcl = React.useRef<AccessControlConfig | undefined>();
    const onChangeProjectAcl = React.useCallback((newProjectAcl: AccessControlConfig) => {
        projectAcl.current = newProjectAcl;
    }, []);
    const isUnmounted = React.useRef(false);
    const importCancelled = React.useRef(false);
    const pendingSleepTimeoutId = React.useRef<number | null>(null);
    const aclManagement = useProjectAclManagementComponent({
        onChange: onChangeProjectAcl,
        labelEmphasis: "strong",
        externalInitialAclGroups: { groups: [] },
    });

    const clearPendingImportTimeout = React.useCallback(() => {
        if (pendingSleepTimeoutId.current != null) {
            clearTimeout(pendingSleepTimeoutId.current);
            pendingSleepTimeoutId.current = null;
        }
    }, []);

    const stopPendingImport = React.useCallback(() => {
        importCancelled.current = true;
        clearPendingImportTimeout();
    }, [clearPendingImportTimeout]);

    const setLoadingIfMounted = React.useCallback((nextLoading: boolean) => {
        if (!isUnmounted.current) {
            setLoading(nextLoading);
        }
    }, []);

    useEffect(() => {
        uppy.use(XHR, {
            method: "POST",
            fieldName: "file",
            metaFields: [],
        });
        uppy.getPlugin("XHRUpload").setOptions({
            endpoint: workspaceApi(`/projectImport`),
        });

        return () => {
            isUnmounted.current = true;
            stopPendingImport();
            uppy.cancelAll();
            uppy.reset();
            uppy.close();
        };
    }, [stopPendingImport, uppy]);

    useEffect(() => {
        if (maxFileUploadSizeBytes) {
            uppy.setOptions({
                restrictions: {
                    maxFileSize: maxFileUploadSizeBytes,
                },
            });
        }
    }, [maxFileUploadSizeBytes, uppy]);

    useEffect(() => {
        if (projectImportId) {
            loadProjectImportDetails(projectImportId);
        }
    }, [projectImportId]);

    const loadProjectImportDetails = async (projectImportId: string) => {
        if (isUnmounted.current) {
            return;
        }
        setProjectDetailsError(null);
        try {
            setLoadingIfMounted(true);
            const response = await requestProjectImportDetails(projectImportId);
            if (!isUnmounted.current) {
                setProjectImportDetails(response.data);
                setDestination("original");
            }
        } catch (ex) {
            if (!isUnmounted.current) {
                setProjectDetailsError(" " + errorDetails(ex));
            }
        } finally {
            setLoadingIfMounted(false);
        }
    };

    const closeDialog = async () => {
        stopPendingImport();
        await cleanUp();
        close();
    };

    const goBack = async () => {
        stopPendingImport();
        await cleanUp();
        back?.();
    };

    // Deletes the uploaded file in the backend
    const cleanUp = async () => {
        if (projectImportId) {
            try {
                setLoadingIfMounted(true);
                await requestDeleteProjectImport(projectImportId);
            } catch (ex) {
                // If this fails for whatever reason the backend will remove the file automatically after a specific period
            } finally {
                setLoadingIfMounted(false);
            }
        }
    };

    const handleFileAdded = async () => {
        setUploadError(null);
        await uppy.upload();
    };

    const startProjectImport = async () => {
        const generateNewProjectId = destination === "generated";
        const overWriteExistingProject = replacingProject;
        setStartProjectImportExecutionError(null);
        if (projectImportId) {
            importCancelled.current = false;
            try {
                setLoadingIfMounted(true);
                await requestStartProjectImport(
                    projectImportId,
                    generateNewProjectId,
                    overWriteExistingProject,
                    overWriteExistingProject ? undefined : projectAcl.current?.groups,
                    destination === "custom" ? customProjectId : undefined,
                );
                let status: Partial<IProjectExecutionStatus> = {};
                const sleep = (ms: number) =>
                    new Promise<void>((resolve) => {
                        pendingSleepTimeoutId.current = window.setTimeout(() => {
                            pendingSleepTimeoutId.current = null;
                            resolve();
                        }, ms);
                    });
                let errorCounter = 0;
                while (!status.importEnded && !importCancelled.current) {
                    try {
                        status = (await requestProjectImportExecutionStatus(projectImportId)).data;
                        errorCounter = 0;
                    } catch (err) {
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
                    setStartProjectImportExecutionError(status.failureMessage ?? t("ProjectImportModal.importFailed"));
                }
            } catch (ex) {
                if (!importCancelled.current && !isUnmounted.current) {
                    setStartProjectImportExecutionError(errorDetails(ex) || t("ProjectImportModal.importFailed"));
                    if (destination === "custom" && (ex.httpStatus === 409 || ex.httpStatus === 400)) {
                        setValidatedProjectId(null);
                        setCustomIdError(
                            t(
                                ex.httpStatus === 409
                                    ? "ProjectImportModal.projectIdAlreadyExists"
                                    : "CreateModal.CustomIdentifierInput.validations.invalid",
                            ),
                        );
                    }
                }
            } finally {
                clearPendingImportTimeout();
                setLoadingIfMounted(false);
            }
        }
    };

    // Extracts the error details from an exception
    const errorDetails = (error): string => {
        let details = error?.message ? String(error.message) : "";
        const idx = details.indexOf("Source error");
        if (idx > 0) {
            details = details.substring(0, idx);
        }
        return details;
    };

    const handleApproveReplacement = () => {
        setApproveReplacement(!approveReplacement);
    };

    const handleUploadError = (fileData, error) => {
        let details = errorDetails(error);
        setUploadError(
            t("ProjectImportModal.responseUploadError", {
                file: fileData.name,
                details: details,
            }),
        );
        uppy.reset();
    };
    const onUploadSuccess = (file: UppyFile, response) => {
        const nextProjectImportId = response?.body?.projectImportId;
        if (nextProjectImportId) {
            if (projectImportDetails?.errorMessage && projectImportId) {
                void requestDeleteProjectImport(projectImportId).catch(() => {
                    // The backend removes abandoned uploads automatically if deletion fails.
                });
            }
            setProjectImportDetails(null);
            setProjectImportId(nextProjectImportId);
        } else {
            setUploadError(t("ProjectImportModal.responseInvalid"));
            uppy.reset();
        }
    };
    const uploader = (
        <UploadNewFile
            uppy={uppy}
            allowMultiple={false}
            onAdded={handleFileAdded}
            onUploadSuccess={onUploadSuccess}
            onUploadError={handleUploadError}
            uploadEndpoint={workspaceApi(`/projectImport`)}
            attachFileNameToEndpoint={false}
        />
    );
    const actions: React.JSX.Element[] = [];
    if (projectImportDetails && !projectImportDetails.errorMessage) {
        actions.push(
            <Button
                data-test-id={replacingProject ? "replaceImportProjectBtn" : "startImportProjectBtn"}
                key="importProject"
                affirmative={!replacingProject}
                disruptive={replacingProject}
                disabled={
                    loading ||
                    (replacingProject && (!approveReplacement || !!projectImportDetails.noAccess)) ||
                    (destination === "custom" &&
                        (!customProjectId || validatedProjectId !== customProjectId || !!customIdError))
                }
                onClick={startProjectImport}
            >
                {t(replacingProject ? "ProjectImportModal.replaceImportBtn" : "ProjectImportModal.importBtn")}
            </Button>,
        );
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

    const uploaderElement = (
        <FieldItem
            key={"projectFile"}
            labelProps={{
                text: t("ProjectImportModal.projectFile"),
                htmlFor: "projectFile-input",
            }}
            intent={uploadError !== null ? "danger" : undefined}
            messageText={uploadError !== null ? uploadError : undefined}
        >
            {uploader}
        </FieldItem>
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
                <Spacing />
                <FieldItem
                    labelProps={{
                        text: t("ProjectImportModal.destination"),
                        id: "import-destination-label",
                        emphasis: "strong",
                    }}
                >
                    <div role="radiogroup" aria-labelledby="import-destination-label" data-test-id="importDestination">
                        <RadioButton
                            name="import-destination"
                            data-test-id="importDestinationOriginal"
                            checked={destination === "original"}
                            onChange={() => changeDestination("original")}
                            label={t("ProjectImportModal.destinationOriginal", { id: details.projectId })}
                        />
                        <RadioButton
                            name="import-destination"
                            data-test-id="importDestinationGenerated"
                            checked={destination === "generated"}
                            onChange={() => changeDestination("generated")}
                            label={t("ProjectImportModal.destinationGenerated")}
                        />
                        <RadioButton
                            name="import-destination"
                            data-test-id="importDestinationCustom"
                            checked={destination === "custom"}
                            onChange={() => changeDestination("custom")}
                            label={t("ProjectImportModal.destinationCustom")}
                        />
                    </div>
                </FieldItem>
                {destination === "custom" && (
                    <FieldItem
                        data-test-id="customProjectIdField"
                        labelProps={{
                            text: t("CreateModal.CustomIdentifierInput.ProjectId"),
                            htmlFor: "import-custom-id",
                        }}
                        intent={customIdError ? "danger" : undefined}
                        messageText={customIdError ?? undefined}
                        helperText={t("CreateModal.CustomIdentifierInput.helperDescription")}
                    >
                        <TextField
                            id="import-custom-id"
                            data-test-id="customProjectIdInput"
                            value={customProjectId}
                            required
                            intent={customIdError ? "danger" : undefined}
                            onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                                setCustomProjectId(event.target.value);
                                setValidatedProjectId(null);
                                setCustomIdError(null);
                                setStartProjectImportExecutionError(null);
                            }}
                        />
                    </FieldItem>
                )}
                {replacingProject && projectExistsNotification(details)}
                {!replacingProject && aclManagement.component && (
                    <div data-test-id="importProjectGroups">
                        <Spacing />
                        {aclManagement.component}
                    </div>
                )}
                {startProjectImportExecutionError && (
                    <Notification
                        data-test-id="projectImportError"
                        intent="danger"
                        message={startProjectImportExecutionError}
                    />
                )}
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
                                  target={"_blank"}
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
                {!cannotOverwrite && (
                    <Checkbox
                        data-test-id={"replaceExistingProjectCheckBox"}
                        inline={true}
                        checked={approveReplacement}
                        onChange={handleApproveReplacement}
                    >
                        <strong>{t("ProjectImportModal.replaceImportBtn")}</strong>
                    </Checkbox>
                )}
            </Notification>
        );
    };

    const projectDetailElement = (details: IProjectImportDetails) => {
        if (details.errorMessage) {
            return (
                <>
                    <Notification
                        intent="danger"
                        message={t("ProjectImportModal.invalidArchive", { details: details.errorMessage })}
                    />
                    <Spacing />
                    {uploaderElement}
                </>
            );
        } else {
            return projectDetails(details);
        }
    };

    const errorRetryElement = (errorMessage: string, retryAction: () => any) => {
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
            t("ProjectImportModal.detailsFailed", { details: projectDetailsError }),
            () => projectImportId && loadProjectImportDetails(projectImportId),
        )
    ) : projectImportDetails ? (
        projectDetailElement(projectImportDetails)
    ) : (
        uploaderElement
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
