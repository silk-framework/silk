import React, { useEffect, useState } from "react";
import { Button, CardActionsAux, FieldItem, SimpleDialog } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { UploadNewFile } from "../FileUploader/cases/UploadNewFile";
import Uppy from "@uppy/core";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import XHR from "@uppy/xhr-upload";
import { requestProjectImportDetails } from "@ducks/workspace/requests";
import { IProjectImportDetails } from "@ducks/workspace/typings";
import { Loading } from "../Loading/Loading";

interface IProps {
    // Called when closing the modal
    close: () => void;
    // Optional back action
    back?: () => void;
}

export function ProjectImportModal({ close, back }: IProps) {
    const [t] = useTranslation();
    const [uppy] = useState(Uppy());
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

    const handleFileAdded = async () => {
        await uppy.upload();
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
        actions.push(
            <Button
                data-test-id={"importProjectBtn"}
                key="importProject"
                affirmative={true}
                onClick={TODO}
                disabled={false}
            >
                {t("ProjectImportModal.startImportBtn")}
            </Button>
        );
    }
    // Add 'Cancel' button
    actions.push(
        <Button key="cancel" onClick={close}>
            {t("common.action.cancel")}
        </Button>
    );
    // Add 'Back' button
    actions.push(
        <CardActionsAux key="aux">
            {back && (
                <Button key="back" onClick={back}>
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
