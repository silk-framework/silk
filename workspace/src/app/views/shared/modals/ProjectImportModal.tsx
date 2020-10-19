import React, { useEffect, useState } from "react";
import {
    Button,
    CardActionsAux,
    Checkbox,
    FieldItem,
    Notification,
    SimpleDialog,
    PropertyValueList,
    PropertyValuePair,
    PropertyName,
    PropertyValue,
    Spacing,
    TitleSubsection,
} from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import Uppy, { UppyFile } from "@uppy/core";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import XHR from "@uppy/xhr-upload";
import {
    requestDeleteProjectImport,
    requestProjectImportDetails,
    requestProjectImportExecutionStatus,
    requestStartProjectImport,
} from "@ducks/workspace/requests";
import { IProjectExecutionStatus, IProjectImportDetails } from "@ducks/workspace/typings";
import { Loading } from "../Loading/Loading";
import { useDispatch } from "react-redux";
import { routerOp } from "@ducks/router";
import { ContentBlobToggler } from "../ContentBlobToggler/ContentBlobToggler";
import { absoluteProjectPath } from "../../../utils/routerUtils";
import ReactMarkdown from "react-markdown";
import { firstNonEmptyLine } from "../ContentBlobToggler";
import { UploadNewFile } from "../FileUploader/cases/UploadNewFile/UploadNewFile";

interface IProps {
    // Called when closing the modal
    close: () => void;
    // Optional back action
    back?: () => void;
}

export function ProjectImportModal({ close, back }: IProps) {
    const [t] = useTranslation();
    const [uppy] = useState(Uppy());
    const dispatch = useDispatch();
    const [loading, setLoading] = useState(false);
    const [projectImportId, setProjectImportId] = useState<string | null>(null);
    const [projectImportDetails, setProjectImportDetails] = useState<IProjectImportDetails | null>(null);
    const [approveReplacement, setApproveReplacement] = useState(false);
    // Unexpected error for the file upload request
    const [uploadError, setUploadError] = useState<string | null>(null);
    // Unexpected error for the project details request
    const [projectDetailsError, setProjectDetailsError] = useState<string | null>(null);
    // Unexpected error for the project import execution request
    const [startProjectImportExecutionError, setStartProjectImportExecutionError] = useState<
        [string, boolean, boolean] | null
    >(null);

    useEffect(() => {
        uppy.use(XHR, {
            method: "POST",
            fieldName: "file",
            metaFields: [],
        });
        uppy.getPlugin("XHRUpload").setOptions({
            endpoint: workspaceApi(`/projectImport`),
        });
    }, []);

    useEffect(() => {
        if (projectImportId) {
            loadProjectImportDetails(projectImportId);
        }
    }, [projectImportId]);

    const loadProjectImportDetails = async (projectImportId: string) => {
        setProjectDetailsError(null);
        try {
            setLoading(true);
            const response = await requestProjectImportDetails(projectImportId);
            setProjectImportDetails(response.data);
        } catch (ex) {
            setProjectDetailsError(" " + errorDetails(ex));
        } finally {
            setLoading(false);
        }
    };

    const closeDialog = async () => {
        await cleanUp();
        close();
    };

    const goBack = async () => {
        await cleanUp();
        back();
    };

    // Deletes the uploaded file in the backend
    const cleanUp = async () => {
        if (projectImportId) {
            try {
                setLoading(true);
                await requestDeleteProjectImport(projectImportId);
            } catch (ex) {
                // If this fails for whatever reason the backend will remove the file automatically after a specific period
            } finally {
                setLoading(false);
            }
        }
    };

    const handleFileAdded = async () => {
        setUploadError(null);
        await uppy.upload();
    };

    const startProjectImport = async (generateNewProjectId: boolean, overWriteExistingProject: boolean) => {
        setStartProjectImportExecutionError(null);
        if (projectImportId) {
            try {
                setLoading(true);
                await requestStartProjectImport(projectImportId, generateNewProjectId, overWriteExistingProject);
                let status: Partial<IProjectExecutionStatus> = {};
                while (!status.importEnded) {
                    status = (await requestProjectImportExecutionStatus(projectImportId)).data;
                }
                close();
                dispatch(routerOp.goToPage(`projects/${status.projectId}`));
            } catch (ex) {
                setStartProjectImportExecutionError([
                    " " + errorDetails(ex),
                    generateNewProjectId,
                    overWriteExistingProject,
                ]);
            } finally {
                setLoading(false);
            }
        }
    };

    // Extracts the error details from an exception
    const errorDetails = (error): string => {
        let details = error?.message ? ` Details: ${error.message}` : "";
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
            t("ProjectImportModal.responseUploadError", "File {{file}} could not be uploaded! {{details}}", {
                file: <code>fileData.name</code>,
                details: details,
            })
        );
        uppy.reset();
    };
    const onUploadSuccess = (file: UppyFile, response) => {
        const projectImportId = response?.body?.projectImportId;
        if (projectImportId) {
            setProjectImportId(projectImportId);
        } else {
            setUploadError(
                t(
                    "ProjectImportModal.responseInvalid",
                    "Invalid response received from project upload. Project import cannot proceed."
                )
            );
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
    const actions: JSX.Element[] = [];
    if (projectImportDetails) {
        if (!projectImportDetails.errorMessage && !projectImportDetails.projectAlreadyExists) {
            actions.push(
                <Button
                    data-test-id={"startImportProjectBtn"}
                    key="importProject"
                    affirmative={true}
                    onClick={() => startProjectImport(false, false)}
                    disabled={false}
                >
                    {t("ProjectImportModal.importBtn")}
                </Button>
            );
        } else if (projectImportDetails.projectAlreadyExists) {
            approveReplacement
                ? actions.push(
                      <Button
                          data-test-id={"replaceImportProjectBtn"}
                          key="replaceProject"
                          disruptive={true}
                          onClick={() => startProjectImport(false, true)}
                          disabled={false}
                      >
                          {t("ProjectImportModal.replaceImportBtn")}
                      </Button>
                  )
                : actions.push(
                      <Button
                          data-test-id={"importUnderFreshIdBtn"}
                          key="importAsFreshProject"
                          affirmative={true}
                          onClick={() => startProjectImport(true, false)}
                          disabled={false}
                      >
                          {t("ProjectImportModal.importUnderFreshIdBtn")}
                      </Button>
                  );
        }
    }
    // Add 'Cancel' button
    actions.push(
        <Button key="cancel" onClick={closeDialog}>
            {t("common.action.cancel")}
        </Button>
    );
    // Add 'Back' button
    actions.push(
        <CardActionsAux key="aux">
            {back && (
                <Button key="back" onClick={goBack}>
                    {t("common.words.back", "Back")}
                </Button>
            )}
        </CardActionsAux>
    );

    const uploaderElement = (
        <FieldItem
            key={"projectFile"}
            labelAttributes={{
                text: t("ProjectImportModal.projectFile"),
                htmlFor: "projectFile-input",
            }}
            hasStateDanger={uploadError !== null}
            messageText={uploadError}
        >
            {uploader}
        </FieldItem>
    );

    const projectDetails = (details: IProjectImportDetails) => (
        <>
            <TitleSubsection>{t("ProjectImportModal.importSummary", "Imported project summary")}</TitleSubsection>
            <PropertyValueList>
                {!!details.label && (
                    <PropertyValuePair hasDivider key={"label"}>
                        <PropertyName>{t("form.field.label", "Label")}</PropertyName>
                        <PropertyValue>{details.label}</PropertyValue>
                    </PropertyValuePair>
                )}
                {!!details.description && (
                    <PropertyValuePair hasSpacing hasDivider>
                        <PropertyName>{t("form.field.description", "Description")}</PropertyName>
                        <PropertyValue>
                            <ContentBlobToggler
                                className="di__dataset__metadata-description"
                                contentPreview={details.description}
                                previewMaxLength={128}
                                contentFullview={details.description}
                                renderContentFullview={(content) => {
                                    return <ReactMarkdown source={details.description} />;
                                }}
                                renderContentPreview={firstNonEmptyLine}
                            />
                        </PropertyValue>
                    </PropertyValuePair>
                )}
            </PropertyValueList>
        </>
    );

    const projectDetailElement = (details: IProjectImportDetails) => {
        if (details.projectAlreadyExists) {
            return (
                <>
                    <Notification
                        warning={true}
                        actions={[
                            <Button href={absoluteProjectPath(details.projectId)} target={"_empty"}>
                                {t("ProjectImportModal.openExistingProject", "Open existing project page")}
                            </Button>,
                        ]}
                    >
                        <p>
                            {t(
                                "ProjectImportModal.warningExistingProject",
                                "A project with the same ID already exists! Choose to either overwrite the existing project or import the project under a freshly generated ID."
                            )}
                        </p>
                        <Spacing />
                        <Checkbox inline={true} checked={approveReplacement} onChange={handleApproveReplacement}>
                            <strong>{t("ProjectImportModal.replaceImportBtn")}</strong>
                        </Checkbox>
                    </Notification>
                    <Spacing />
                    {projectDetails(details)}
                </>
            );
        } else if (details.errorMessage) {
            return (
                <Notification
                    danger={true}
                    message={"The project cannot be imported. Details: " + details.errorMessage}
                />
            );
        } else {
            return projectDetails(details);
        }
    };

    const errorRetryElement = (errorMessage: string, retryAction: () => any) => {
        return (
            <>
                <Notification danger={true} message={errorMessage} />
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
        <Loading />
    ) : projectDetailsError !== null ? (
        errorRetryElement("Failed to retrieve project import details. " + projectDetailsError, () =>
            loadProjectImportDetails(projectImportId)
        )
    ) : startProjectImportExecutionError ? (
        errorRetryElement(
            "Failed to start execution of the project import! " + startProjectImportExecutionError[0],
            () => startProjectImport(startProjectImportExecutionError[1], startProjectImportExecutionError[2])
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
