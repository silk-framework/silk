import React, { useEffect, useState } from "react";
import {
    Button,
    CardActionsAux,
    FieldItem,
    Link,
    Notification,
    SimpleDialog,
    Spacing,
    TextField,
    WhiteSpaceContainer,
} from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { UploadNewFile } from "../FileUploader/cases/UploadNewFile";
import Uppy from "@uppy/core";
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
import { SERVE_PATH } from "../../../constants/path";
import { workspacePath } from "../../../../../test/integration/TestHelper";
import { absoluteProjectPath } from "../../../utils/routerUtils";
import ReactMarkdown from "react-markdown";
import { firstNonEmptyLine } from "../ContentBlobToggler";

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
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const TODO = () => console.log("TODO");

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
        try {
            setLoading(true);
            const response = await requestProjectImportDetails(projectImportId);
            setProjectImportDetails(response.data);
        } catch (ex) {
            // TODO: Handle errors
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
                // TODO
            } finally {
                setLoading(false);
            }
        }
    };

    const handleFileAdded = async () => {
        await uppy.upload();
    };

    const startProjectImport = async (generateNewProjectId: boolean, overWriteExistingProject: boolean) => {
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
                // TODO
                console.log(ex);
            } finally {
                setLoading(false);
            }
        }
    };

    const handleUploadError = (fileData, error) => {
        let errorDetails = error?.message ? ` Details: ${error.message}` : "";
        const idx = errorDetails.indexOf("Source error");
        if (idx > 0) {
            errorDetails = errorDetails.substring(0, idx);
        }
        setErrorMessage(`File '${fileData.name}' could not be uploaded!${errorDetails}`);
        uppy.reset();
    };
    const onUploadSuccess = (file: File, response) => {
        const projectImportId = response?.body?.projectImportId;
        if (projectImportId) {
            setProjectImportId(projectImportId);
        } else {
            setErrorMessage("Invalid response received from project upload. Project import cannot proceed.");
            uppy.reset();
        }
    };
    const uploader = (
        <UploadNewFile
            uppy={uppy}
            simpleInput={false}
            allowMultiple={false}
            onAdded={handleFileAdded}
            onProgress={TODO}
            onUploadSuccess={onUploadSuccess}
            onUploadError={handleUploadError}
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
                    {t("ProjectImportModal.startImportBtn")}
                </Button>
            );
        } else if (projectImportDetails.projectAlreadyExists) {
            actions.push(
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
            actions.push(
                <Button
                    data-test-id={"replaceImportProjectBtn"}
                    key="replaceProject"
                    disruptive={true}
                    onClick={() => startProjectImport(false, true)}
                    disabled={false}
                >
                    {t("ProjectImportModal.replaceImportBtn")}
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
            hasStateDanger={errorMessage !== null}
            messageText={errorMessage}
        >
            {uploader}
        </FieldItem>
    );

    const projectDetails = (details: IProjectImportDetails) => (
        <>
            <h2>Project details</h2>
            <Spacing />
            <FieldItem
                key={"label"}
                labelAttributes={{
                    text: t("form.field.label"),
                }}
            >
                <WhiteSpaceContainer marginTop="tiny" marginRight="xlarge" marginBottom="small" marginLeft="regular">
                    {details.label}
                </WhiteSpaceContainer>
            </FieldItem>
            {details.description && (
                <FieldItem
                    key={"description"}
                    labelAttributes={{
                        text: t("form.field.description"),
                    }}
                >
                    <WhiteSpaceContainer
                        marginTop="tiny"
                        marginRight="xlarge"
                        marginBottom="small"
                        marginLeft="regular"
                    >
                        <ContentBlobToggler
                            contentPreview={details.description}
                            contentFullview={details.description}
                            previewMaxLength={128}
                            renderContentFullview={(content) => {
                                return <ReactMarkdown source={details.description} />;
                            }}
                            renderContentPreview={firstNonEmptyLine}
                        />
                    </WhiteSpaceContainer>
                </FieldItem>
            )}
        </>
    );

    const projectDetailElement = (details: IProjectImportDetails) => {
        if (details.projectAlreadyExists) {
            return (
                <>
                    <Notification
                        warning={true}
                        message={
                            "A project with the same ID already exists! Choose to either overwrite " +
                            "the existing project or import the project under a freshly generated ID."
                        }
                    />
                    <Spacing />
                    <Link href={absoluteProjectPath(details.projectId)} target={"_empty"}>
                        Open existing project page
                    </Link>
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
            return;
        }
    };

    const dialogItem = loading ? (
        <Loading />
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
