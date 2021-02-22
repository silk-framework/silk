import React, { useDebugValue, useState } from "react";
import { Button, SimpleDialog } from "@gui-elements/index";
import FileUploader from "../../FileUploader";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { IUploaderOptions } from "../../FileUploader/FileSelectionMenu";
import { useTranslation } from "react-i18next";

export interface IFileUploadModalProps {
    isOpen: boolean;

    onDiscard(): void;

    uploaderOptions?: Partial<IUploaderOptions>;
}

export function FileUploadModal({ isOpen, onDiscard, uploaderOptions = {} }: IFileUploadModalProps) {
    const { maxFileUploadSize } = useSelector(commonSel.initialSettingsSelector);
    const [fileUploaderInstance, setFileUploaderInstance] = useState<any>(null);

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

    /**
     * @override uploader change function and handle it
     */
    const overriddenUploaderOptions = {
        ...uploaderOptions,
        projectId,
    };

    return (
        <>
            <SimpleDialog
                data-test-id="file-upload-dialog"
                title={t("FileUploader.modalTitle", "Upload file")}
                size="small"
                isOpen={isOpen}
                onClose={handleDiscard}
                preventSimpleClosing={true}
                actions={
                    <Button data-test-id="file-upload-dialog-close-btn" key="close" onClick={onDiscard}>
                        Close
                    </Button>
                }
            >
                <FileUploader
                    getInstance={getUploaderInstance}
                    projectId={projectId}
                    {...overriddenUploaderOptions}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                />
            </SimpleDialog>
        </>
    );
}
