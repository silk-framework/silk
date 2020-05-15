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
import { isDevelopment } from "../../../../constants/path";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    uploaderOptions?: IUploaderOptions;
}

export function FileUploadModal({ isOpen, onDiscard, uploaderOptions = {} }: IFileUploadModalProps) {
    /**
     * Selected filename, for both 3 options
     */
    const [selectedFilenames, setSelectedFilename] = useState("");

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
        setSelectedFilename("");
        fileUploaderInstance.reset();
    };

    const onFileAdded = async (result: any) => {
        try {
            setIsCheckingFile(true);
            const isExists = await isResourceExists(result.name);

            isExists ? setInvokeOverrideDialog(result) : upload(result);
        } finally {
            setIsCheckingFile(false);
        }
    };

    const upload = async (file: any) => {
        fileUploaderInstance.setEndpoint(`${uploadUrl}/${file.name}`);
        setIsUploading(true);
        try {
            await fileUploaderInstance.upload();
            resetFileDialog();

            setSelectedFilename(file.name);
        } catch (e) {
            console.log(e);
        } finally {
            setIsUploading(false);
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

    const handleUploaderChange = (value: any) => {
        if (value && value.data instanceof File) {
            onFileAdded(value);
        } else if (typeof value === "string") {
            setSelectedFilename(value);
        } else if (isDevelopment) {
            console.error("Uploaded Result not handled, please take a look into FileUploader, onAdded event");
        }
    };

    const handleApply = () => {
        uploaderOptions.onChange(selectedFilenames);
        resetFileDialog();
        onDiscard();
    };

    /**
     * @override uploader change function and handle it
     */
    const overriddenUploaderOptions = {
        ...uploaderOptions,
        onChange: handleUploaderChange,
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
                        [
                            <Button key="apply" onClick={handleApply}>
                                Apply
                            </Button>,
                            <Button key="close" onClick={onDiscard}>
                                Close
                            </Button>,
                        ]
                    )
                }
            >
                <FileUploader
                    getInstance={getUploaderInstance}
                    loading={isCheckingFile}
                    {...overriddenUploaderOptions}
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
