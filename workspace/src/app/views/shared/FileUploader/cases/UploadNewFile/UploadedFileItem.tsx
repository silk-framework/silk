import React from "react";
import { Button, Notification, ProgressBar, Spacing } from "@eccenca/gui-elements";
import { UppyFile } from "@uppy/core";
import { useTranslation } from "react-i18next";
import { Intent } from "@eccenca/gui-elements/blueprint/constants";

interface IProps {
    file: UppyFile;

    onRemoveFile?(file: UppyFile);
}

export function UploadedFileItem({ file, onRemoveFile }: IProps) {
    const [t] = useTranslation();

    return (
        <div key={file.id}>
            <Notification
                success={true}
                actions={
                    onRemoveFile ? (
                        <Button outlined onClick={() => onRemoveFile(file)}>
                            {t("common.action.DeleteSmth", { smth: " " })}
                        </Button>
                    ) : undefined
                }
            >
                <p>{t("FileUploader.successfullyUploaded", { uploadedName: file.name })}</p>
                <Spacing />
                <ProgressBar value={1} stripes={false} intent={Intent.SUCCESS} />
            </Notification>
            <Spacing />
        </div>
    );
}
