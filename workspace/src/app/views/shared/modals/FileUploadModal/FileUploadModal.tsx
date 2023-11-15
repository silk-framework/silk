import React, { useDebugValue, useState } from "react";
import { Button, SimpleDialog } from "@eccenca/gui-elements";
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
    const [allSuccessful, setAllSuccessful] = React.useState(true);

    const projectId = useSelector(commonSel.currentProjectIdSelector);
    const [t] = useTranslation();

    useDebugValue(!projectId ? "Project ID not provided and upload url is not valid" : "");

    const allFilesSuccessfullyUploadedHandler = React.useCallback((allSuccessful: boolean) => {
        setAllSuccessful(allSuccessful);
    }, []);

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
                preventSimpleClosing={!allSuccessful}
                actions={
                    <Button data-test-id="file-upload-dialog-close-btn" key="close" onClick={onDiscard}>
                        {t("common.action.close", "Close")}
                    </Button>
                }
            >
                <FileUploader
                    projectId={projectId}
                    getInstance={getUploaderInstance}
                    {...uploaderOptions}
                    onChange={() => {
                        /** We are not interested on file changes, only upload. */
                    }}
                    maxFileUploadSizeBytes={maxFileUploadSize}
                    allFilesSuccessfullyUploadedHandler={allFilesSuccessfullyUploadedHandler}
                />
            </SimpleDialog>
        </>
    );
}
