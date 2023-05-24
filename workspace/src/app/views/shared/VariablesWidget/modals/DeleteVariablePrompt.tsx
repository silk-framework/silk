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
            title="Delete Variable"
            isOpen={isOpen}
            onClose={closeModal}
            notifications={errorMessage ? <Notification danger>{errorMessage}</Notification> : null}
            actions={[
                <Button
                    key="copy"
                    disruptive
                    onClick={deleteVariable}
                    disabled={isDeletingVariable}
                    loading={isDeletingVariable}
                >
                    {t("widget.VariableWidget.actions.delete", "Delete")}
                </Button>,
                <Button key="cancel" onClick={closeModal}>
                    No, thanks
                </Button>,
            ]}
        >
            <p>Are you sure you want to delete this variable?</p>
        </SimpleDialog>
    );
};

export default DeleteVariablePrompt;
