import { Button, Notification } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

export function EmptyFileWidget({ onFileAdd }) {
    const [t] = useTranslation();

    return (
        <div data-test-id={"project-files-widget-empty"}>
            <Notification
                actions={
                    <Button data-test-id="project-files-widget-add-file-btn" onClick={onFileAdd} affirmative>
                        {t("common.action.AddSmth", { smth: t("widget.FileWidget.file", "File") })}
                    </Button>
                }
            >
                {t("widget.FileWidget.empty", "No files are found, add them here to use it later")}
            </Notification>
        </div>
    );
}
