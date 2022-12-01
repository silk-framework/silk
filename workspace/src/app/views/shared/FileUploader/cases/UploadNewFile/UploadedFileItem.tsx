import { Button, Notification, ProgressBar, Spacing } from "@eccenca/gui-elements";
import { Intent } from "@eccenca/gui-elements/blueprint/constants";
import { UppyFile } from "@uppy/core";
import React from "react";
import { useTranslation } from "react-i18next";

interface IProps {
    file: UppyFile;

    onRemoveFile(file: UppyFile);
}

export function UploadedFileItem({ file, onRemoveFile }: IProps) {
    const [t] = useTranslation();

    return (
        <div key={file.id}>
            <Notification
                success={true}
                actions={
                    <Button outlined onClick={() => onRemoveFile(file)}>
                        {t("common.action.DeleteSmth", { smth: " " })}
                    </Button>
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
