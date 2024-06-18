import React from "react";
import { Button, Notification, Spacing } from "@eccenca/gui-elements";
import { UppyFile } from "@uppy/core";
import { useTranslation } from "react-i18next";

interface IProps {
    file: UppyFile & { error?: string };

    onCancelRetry(fileId: string);

    onRetry?(fileId: string);
}

export function RetryFileItem({ file, onCancelRetry, onRetry }: IProps) {
    const [t] = useTranslation();

    return (
        <div key={file.id}>
            <Notification
                onDismiss={(didTimeoutExpire) => !didTimeoutExpire && onCancelRetry(file.id)}
                danger={true}
                actions={
                    onRetry && (
                        <Button outlined onClick={() => onRetry(file.id)}>
                            {t("FileUploader.retry", "Retry")}
                        </Button>
                    )
                }
            >
                <p>{file.error || t("FileUploader.retryAbortMessage", { fileName: file.name })}</p>
                <Spacing />
            </Notification>
            <Spacing />
        </div>
    );
}
