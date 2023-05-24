import React from "react";
import { Button, SimpleDialog } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";

interface DeleteVariablePromptProps {
    isOpen: boolean;
    closeModal: () => void;
    deleteVariable: () => void;
    isDeletingVariable: boolean;
}

const DeleteVariablePrompt: React.FC<DeleteVariablePromptProps> = ({
    isOpen,
    closeModal,
    deleteVariable,
    isDeletingVariable,
}) => {
    const [t] = useTranslation();

    return (
        <SimpleDialog
            data-test-id={"copy-item-to-modal"}
            size="small"
            title="Delete Variable"
            isOpen={isOpen}
            onClose={closeModal}
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
