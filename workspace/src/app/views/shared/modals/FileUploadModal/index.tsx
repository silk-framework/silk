import React, { useState } from "react";
import {
    Button,
    SimpleDialog,
} from '@wrappers/index';
import AbortAlert from "./AbortAlert";
import OverrideAlert from "./OverrideAlert";
import FileUploader from "../../FileUploader";

export interface IFileUploadModalProps {
    isOpen: boolean;

    uploadUrl: string;

    onDiscard(): void;

    onCheckFileExists?(fileName: string);
}

export default function FileUploadModal({isOpen, onDiscard, onCheckFileExists, uploadUrl}: IFileUploadModalProps) {
    const [fileUploaderInstance, setFileUploaderInstance] = useState<any>(null);
    const [isCheckingFile, setIsCheckingFile] = useState<boolean>(false);
    const [isUploading, setIsUploading] = useState<boolean>(false);
    const [openAbortDialog, setOpenAbortDialog] = useState<boolean>(false);
    const [invokeOverrideDialog, setInvokeOverrideDialog] = useState<File>(null);

    const getUploaderInstance = (instance) => {
        setFileUploaderInstance(instance);
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
    };

    const onFileAdded = async (result: File) => {
        if (onCheckFileExists) {
            setIsCheckingFile(true);
            const isExists = await onCheckFileExists(result.name);
            setIsCheckingFile(false);

            isExists
                ? setInvokeOverrideDialog(result)
                : upload(result)
        } else {
            upload(result);
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

    return <>
        <SimpleDialog
            title="Upload New File"
            isOpen={isOpen}
            onClose={handleDiscard}
            actions={
                isUploading ?
                    <Button
                        onClick={handleDiscard}
                    >
                        Abort Upload
                    </Button> : <Button onClick={onDiscard}>
                        Close
                    </Button>
            }
        >
            <FileUploader
                getInstance={getUploaderInstance}
                onFileAdded={onFileAdded}
                disabled={isCheckingFile}
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
}
