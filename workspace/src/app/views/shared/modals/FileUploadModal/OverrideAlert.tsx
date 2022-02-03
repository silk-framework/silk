import React from "react";
import { AlertDialog, Button } from "gui-elements";
import { useTranslation } from "react-i18next";

interface IProps {
    fileName: string;
    onConfirm: (e) => void;
    onCancel: (e) => void;
    isOpen: boolean;
}

/**
 * @deprecated
 * can be removed
 */
/** Alert to warn against overwriting an existing file */
const OverrideAlert = ({ fileName, isOpen, onCancel, onConfirm }: IProps) => {
    const [t] = useTranslation();

    return (
        <AlertDialog
            warning
            isOpen={isOpen}
            actions={[
                <Button key="replace" onClick={onConfirm}>
                    {t("common.action.replace", "Replace")}
                </Button>,
                <Button key="cancel" onClick={onCancel}>
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <p>{t("OverwriteModal.overwriteFile", { fileName })}</p>
        </AlertDialog>
    );
};

export default OverrideAlert;
