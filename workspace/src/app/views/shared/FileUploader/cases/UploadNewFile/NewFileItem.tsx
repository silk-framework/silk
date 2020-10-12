import React from "react";
import { Button, Notification, Spacing } from "@gui-elements/index";
import ProgressBar from "@gui-elements/blueprint/progressbar";
import { Intent } from "@gui-elements/blueprint/constants";
import { UppyFile } from "@uppy/core";
import { useTranslation } from "react-i18next";

interface IProps {
    file: UppyFile & { error?: string };

    // The progress of the upload >= 0, <= 1.0
    progress: number;

    onAbort(fileId: string);

    onRemove(fileId: string);
}

export function NewFileItem({ file, progress, onAbort, onRemove }: IProps) {
    const [t] = useTranslation();
    const { error } = file;

    const props: any = {
        danger: !!error,
    };
    if (error) {
        props.onDismiss = () => onRemove(file.id);
    }

    return (
        <div key={file.id}>
            <Notification
                {...props}
                actions={
                    !error &&
                    progress < 1 && (
                        <Button outlined onClick={() => onAbort(file.id)}>
                            {t("FileUploader.abortOnly", "Abort Upload")}
                        </Button>
                    )
                }
            >
                <p>{error ? error : t("FileUploader.waitFor", { fileName: file.name })}</p>
                <Spacing />
                {!error && <ProgressBar value={progress} stripes={true} intent={Intent.PRIMARY} />}
            </Notification>
            <Spacing />
        </div>
    );
}
