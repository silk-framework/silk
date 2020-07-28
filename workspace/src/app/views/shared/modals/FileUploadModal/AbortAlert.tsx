import React from "react";
import { AlertDialog, Button } from "@wrappers/index";
import { useTranslation } from "react-i18next";

const AbortAlert = ({ isOpen, onCancel, onConfirm }) => {
    const [t] = useTranslation();

    return (
        <AlertDialog
            danger
            isOpen={isOpen}
            actions={[
                <Button key="abort" onClick={onConfirm}>
                    {t("common.action.abort", "Abort")}
                </Button>,
                <Button key="cancel" onClick={onCancel}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <p> {t("FileUploader.abortProcess", "Abort upload process?")}</p>
        </AlertDialog>
    );
};

export default AbortAlert;
