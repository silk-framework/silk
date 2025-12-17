import { Button, IconButton, Notification, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";

interface Props {
    onReload: () => any | Promise<any>;
    onClose: () => any;
    errorMessage?: string;
}

/** Simple dialog that allows to reload a failed task without providing any additional data. */
export const FixTaskDataNotFoundModal = ({ onReload, onClose, errorMessage }: Props) => {
    const [t] = useTranslation();
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState(errorMessage);

    const reload = async () => {
        setLoading(true);
        try {
            await onReload();
        } catch (ex) {
            if (ex.isHttpError && (ex.httpStatus === 400 || ex.httpStatus === 404)) {
                setMessage(ex.body?.taskLoadingError?.errorMessage ?? ex.message);
            }
        } finally {
            setLoading(false);
        }
    };
    return (
        <SimpleDialog
            isOpen={true}
            title={t("widget.WarningWidget.FixTaskDataNotFoundModal.title")}
            onClose={onClose}
            actions={<Button onClick={onClose} text={t("common.action.close")} />}
        >
            <Notification
                intent="warning"
                message={
                    <p>
                        {t("widget.WarningWidget.FixTaskDataNotFoundModal.warningMessage", { details: "" })}
                        <Spacing size={"small"} />
                        {message ?? "N/A"}
                    </p>
                }
                actions={[
                    <IconButton
                        name={"item-reload"}
                        onClick={reload}
                        loading={loading}
                        title={t("widget.WarningWidget.FixTaskDataNotFoundModal.reloadButton")}
                    />,
                ]}
            ></Notification>
        </SimpleDialog>
    );
};
