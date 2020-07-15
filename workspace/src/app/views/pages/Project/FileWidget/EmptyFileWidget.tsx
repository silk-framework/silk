import React from "react";
import { Button } from "@wrappers/index";
import { useTranslation } from "react-i18next";

export function EmptyFileWidget({ onFileAdd }) {
    const [t] = useTranslation();

    return (
        <div>
            <p>{t("widget.file.empty", "No files are found, add them here to use it later")}</p>
            <Button kind={"primary"} onClick={onFileAdd}>
                + {t("AddSmth", { smth: t("widget.file.file", "File") })}
            </Button>
        </div>
    );
}
