import React from "react";
import { Button, Notification, SimpleDialog } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface DeleteVariablePromptProps {
    isOpen: boolean;
    closeModal: () => void;
    deleteVariable: () => void;
    isDeletingVariable: boolean;
    errorMessage: string;
}

const DeleteVariablePrompt: React.FC<DeleteVariablePromptProps> = ({
    isOpen,
    closeModal,
    deleteVariable,
    isDeletingVariable,
    errorMessage,
}) => {
    const [t] = useTranslation();

    return (
        <SimpleDialog
            size="small"
            title={t("widget.VariableWidget.modalMessages.deleteModalTitle")}
            isOpen={isOpen}
            onClose={closeModal}
            notifications={errorMessage ? <Notification danger>{errorMessage}</Notification> : null}
            actions={[
                <Button
                    key="delete"
                    disruptive
                    onClick={deleteVariable}
                    disabled={isDeletingVariable}
                    loading={isDeletingVariable}
                >
                    {t("widget.VariableWidget.actions.delete", "Delete")}
                </Button>,
                <Button key="cancel" onClick={closeModal}>
                    {t("widget.VariableWidget.actions.noThanks", "No, Thanks")}
                </Button>,
            ]}
        >
            <p>{t("widget.VariableWidget.modalMessages.deletePrompt")}</p>
        </SimpleDialog>
    );
};

export default DeleteVariablePrompt;
