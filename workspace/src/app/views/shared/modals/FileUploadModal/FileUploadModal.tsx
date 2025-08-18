import React, { useDebugValue, useState } from "react";
import { Button, SimpleDialog } from "@eccenca/gui-elements";
import FileUploader from "../../FileUploader";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { IUploaderOptions } from "../../FileUploader/FileSelectionMenu";
import { useTranslation } from "react-i18next";
import { UppyFile } from "@uppy/core";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    uploaderOptions?: Partial<IUploaderOptions>;
}

export function FileUploadModal({ isOpen, onDiscard, uploaderOptions = {} }: IFileUploadModalProps) {
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const [fileUploaderInstance, setFileUploaderInstance] = useState<any>(null);
    const [uploadedFiles, setUploadedFiles] = useState<UppyFile[]>([]);
    const [isUploading, setIsUploading] = React.useState<boolean>(false);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();

    useDebugValue(!projectId ? "Project ID not provided and upload url is not valid" : "");

    if (!projectId) {
        return null;
    }

    const getUploaderInstance = (instance) => {
        setFileUploaderInstance(instance);
    };

    const handleDiscard = () => {
        fileUploaderInstance.reset();
        onDiscard();
    };

    return (
        <>
            <SimpleDialog
                data-test-id="file-upload-dialog"
                title={t("FileUploader.modalTitle", "Upload file")}
                size="small"
                isOpen={isOpen}
                onClose={handleDiscard}
                preventSimpleClosing={isUploading}
                actions={
                    <Button
                        data-test-id="file-upload-dialog-close-btn"
                        key="close"
                        onClick={onDiscard}
                        disabled={isUploading}
                    >
                        {!uploadedFiles.length
                            ? t("common.action.cancel", "Cancel")
                            : t("common.action.close", "Close")}
                    </Button>
                }
            >
                <FileUploader
                    projectId={projectId}
                    getInstance={getUploaderInstance}
                    {...uploaderOptions}
                    listenToUploadedFiles={setUploadedFiles}
                    onChange={(params) => {
                        /** We are not interested on file changes, only upload. */
                    }}
                    onProgress={(amount) => setIsUploading(amount > 0 && amount < 1)} // between 0 and 1
                    maxFileUploadSizeBytes={maxFileUploadSize}
                    t={t}
                />
            </SimpleDialog>
        </>
    );
}
