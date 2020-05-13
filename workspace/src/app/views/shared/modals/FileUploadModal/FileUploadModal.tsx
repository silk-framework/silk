import React, { useDebugValue, useState } from "react";
import { Button, SimpleDialog } from "@wrappers/index";
import AbortAlert from "./AbortAlert";
import OverrideAlert from "./OverrideAlert";
import FileUploader from "../../FileUploader";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import { IUploaderOptions } from "../../FileUploader/FileUploader";
import { requestIfResourceExists } from "@ducks/workspace/requests";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    onUploaded?(e: any): void;

    uploaderOptions?: IUploaderOptions;
}

export function FileUploadModal({ isOpen, onDiscard, onUploaded, uploaderOptions = {} }: IFileUploadModalProps) {
    const [fileUploaderInstance, setFileUploaderInstance] = useState<any>(null);
    const [isCheckingFile, setIsCheckingFile] = useState<boolean>(false);
    const [isUploading, setIsUploading] = useState<boolean>(false);
    const [openAbortDialog, setOpenAbortDialog] = useState<boolean>(false);
    const [invokeOverrideDialog, setInvokeOverrideDialog] = useState<File>(null);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const uploadUrl = legacyApiEndpoint(`/projects/${projectId}/resources`);

    useDebugValue(!projectId ? "Project ID not provided and upload url is not valid" : "");

    if (!projectId) {
        return null;
    }
    const getUploaderInstance = (instance) => {
        setFileUploaderInstance(instance);
    };

    const isResourceExists = async (fileName: string) => {
        try {
            const res = await requestIfResourceExists(projectId, fileName);
            return !!res.size;
        } catch {
            return false;
        }
    };

    const resetFileDialog = () => {
        setIsCheckingFile(false);
        setIsUploading(false);
        setOpenAbortDialog(false);
        setInvokeOverrideDialog(null);
        fileUploaderInstance.reset();
    };

    const upload = async (file: File) => {
        fileUploaderInstance.setEndpoint(`${uploadUrl}/${file.name}`);
        setIsUploading(true);
        await fileUploaderInstance.upload();
        setIsUploading(false);
        resetFileDialog();

        onUploaded(file.name);
    };

    const onFileAdded = async (result: File) => {
        try {
            setIsCheckingFile(true);
            const isExists = await isResourceExists(result.name);

            isExists ? setInvokeOverrideDialog(result) : upload(result);
        } finally {
            setIsCheckingFile(false);
        }
    };

    const handleDiscard = () => {
        if (isUploading) {
            setOpenAbortDialog(true);
            return false;
        }
        resetFileDialog();
        onDiscard();
    };

    const handleOverrideCancel = () => {
        fileUploaderInstance.reset();
        setInvokeOverrideDialog(null);
    };

    return (
        <>
            <SimpleDialog
                title="Upload New File"
                isOpen={isOpen}
                onClose={handleDiscard}
                actions={
                    isUploading ? (
                        <Button onClick={handleDiscard}>Abort Upload</Button>
                    ) : (
                        <Button onClick={onDiscard}>Close</Button>
                    )
                }
            >
                <FileUploader
                    getInstance={getUploaderInstance}
                    onFileAdded={onFileAdded}
                    loading={isCheckingFile}
                    {...uploaderOptions}
                />
            </SimpleDialog>
            <AbortAlert
                isOpen={openAbortDialog}
                onCancel={() => setOpenAbortDialog(false)}
                onConfirm={resetFileDialog}
            />
            <OverrideAlert
                isOpen={invokeOverrideDialog}
                onCancel={handleOverrideCancel}
                onConfirm={() => upload(invokeOverrideDialog)}
            />
        </>
    );
}
