import React from "react";
import { Button, Notification, Spacing } from "@gui-elements/index";
import ProgressBar from "@gui-elements/blueprint/progressbar";
import { Intent } from "@gui-elements/blueprint/constants";
import { UppyFile } from "@uppy/core";
import { useTranslation } from "react-i18next";

interface IProps {
    file: UppyFile;

    // some error
    error: string;

    // remove file handler for remove action
    onRemoveFile(fileName: string);

    // abort action
    onAbort(fileId: string);

    onDismiss(fileId: string);
}

export function NewFileItem({ file, onRemoveFile, onAbort, onDismiss }: IProps) {
    const [t] = useTranslation();

    const { progress } = file;
    const fileProgress = progress.bytesUploaded / progress.bytesTotal;
    const isUploaded = file.progress.uploadComplete;
    const isUploadStarted = !!file.progress.uploadStarted;
    const fileActionBtn = isUploaded ? (
        <Button outlined onClick={() => onRemoveFile(file.name)}>
            {t("common.action.RemoveSmth", { smth: " " })}
        </Button>
    ) : isUploadStarted ? (
        <Button outlined onClick={() => onAbort(file.id)}>
            {t("FileUploader.abortOnly", "Abort Upload")}
        </Button>
    ) : null;

    return (
        <div key={file.id}>
            <Notification success={isUploaded} actions={fileActionBtn} onDismiss={() => onDismiss(file.id)}>
                <p>
                    {!isUploaded
                        ? t("FileUploader.waitFor", "Wait for finished upload.")
                        : t("FileUploader.successfullyUploaded", { uploadedName: file.name })}
                </p>
                <Spacing />
                {isUploadStarted && (
                    <ProgressBar
                        value={fileProgress}
                        stripes={!isUploaded}
                        intent={isUploaded ? Intent.SUCCESS : Intent.PRIMARY}
                    />
                )}
            </Notification>
            <Spacing />
        </div>
    );
}
