import React from "react";
import { Notification } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

export function EmptyFileWidget() {
    const [t] = useTranslation();

    return (
        <div data-test-id={"project-files-widget-empty"}>
            <Notification>
                {t("widget.FileWidget.empty", "No files are found, add them here to use it later")}
            </Notification>
        </div>
    );
}
