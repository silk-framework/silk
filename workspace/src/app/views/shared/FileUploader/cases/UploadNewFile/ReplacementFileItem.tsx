import React from "react";
import { Button, Notification, Spacing } from "@eccenca/gui-elements";
import { UppyFile } from "@uppy/core";
import { useTranslation } from "react-i18next";

interface IProps {
    file: UppyFile;

    onCancelReplacement(fileId: string);

    onReplace(file: UppyFile);
}

export function ReplacementFileItem({ file, onCancelReplacement, onReplace }: IProps) {
    const [t] = useTranslation();

    return (
        <div key={file.id}>
            <Notification
                timeout={1000 * 60 * 60}  //TODO change back to default 0 for no-dismiss after new release.
                onDismiss={() => onCancelReplacement(file.id)}
                warning={true}
                actions={
                    <Button outlined onClick={() => onReplace(file)}>
                        {t("common.action.replace", "Replace")}
                    </Button>
                }
            >
                <p>{t("OverwriteModal.overwriteFile", { fileName: file.name })}</p>
                <Spacing />
            </Notification>
            <Spacing />
        </div>
    );
}
