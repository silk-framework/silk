import React, { useEffect, useState } from "react";
import { Button, CardActionsAux, FieldItem, SimpleDialog } from "@gui-elements/index";
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

    const dialogItem = loading ? (
        <Loading />
    ) : projectImportDetails ? (
        <div>{projectImportDetails.label}</div>
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
