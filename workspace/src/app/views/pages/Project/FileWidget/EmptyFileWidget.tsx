import React from "react";
import { Button, Notification } from "@gui-elements/index";
import { useTranslation } from "react-i18next";

export function EmptyFileWidget({ onFileAdd }) {
    const [t] = useTranslation();

    return (
        <div data-test-id={"project-files-widget-empty"}>
            <Notification
                actions={
                    <Button data-test-id="project-files-widget-add-file-btn" kind={"primary"} onClick={onFileAdd}>
                        {t("common.action.AddSmth", { smth: t("widget.FileWidget.file", "File") })}
                    </Button>
                }
            >
                {t("widget.FileWidget.empty", "No files are found, add them here to use it later")}
            </Notification>
        </div>
    );
}
