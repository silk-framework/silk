import React, { useDebugValue, useState } from "react";
import { Button, FileUploadFile, SimpleDialog } from "@eccenca/gui-elements";
import FileUploader from "../../FileUploader";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { IUploaderInstance, IUploaderOptions } from "../../FileUploader/FileSelectionMenu";
import { useTranslation } from "react-i18next";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    uploaderOptions?: Partial<IUploaderOptions>;
}

export function FileUploadModal({ isOpen, onDiscard, uploaderOptions = {} }: IFileUploadModalProps) {
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const [fileUploaderInstance, setFileUploaderInstance] = useState<IUploaderInstance | null>(null);
    const [uploadedFiles, setUploadedFiles] = useState<FileUploadFile[]>([]);
    const [isUploading, setIsUploading] = React.useState<boolean>(false);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();

    useDebugValue(!projectId ? "Project ID not provided and upload url is not valid" : "");

    if (!projectId) {
        return null;
    }

    const getUploaderInstance = (instance: IUploaderInstance) => {
        setFileUploaderInstance(instance);
    };

    const handleDiscard = () => {
        fileUploaderInstance?.reset();
        onDiscard();
    };

    return (
        <>
            <SimpleDialog
                data-test-id="file-upload-dialog"
                title={t("FileUploader.modalTitle")}
                size="small"
                isOpen={isOpen}
                onClose={handleDiscard}
                preventSimpleClosing={isUploading}
                actions={
                    <Button
                        data-test-id="file-upload-dialog-close-btn"
                        key="close"
                        onClick={handleDiscard}
                        disabled={isUploading}
                    >
                        {!uploadedFiles.length
                            ? t("common.action.cancel")
                            : t("common.action.close")}
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
                    onUploadStateChange={setIsUploading}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                />
            </SimpleDialog>
        </>
    );
}
