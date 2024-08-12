import { Button, SimpleDialog } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

interface PromptModalProps {
    onClose: () => void;
    isOpen: boolean;
    proceed: () => void;
}

const PromptModal: React.FC<PromptModalProps> = ({ onClose, isOpen, proceed }) => {
    const [t] = useTranslation();
    return (
        <SimpleDialog
            data-test-id="project-tab-prompt-modal"
            title="Warning"
            size="small"
            isOpen={isOpen}
            onClose={onClose}
            intent="warning"
            actions={[
                <Button key="proceed" disruptive={true} onClick={proceed} id="prompt-proceed">
                    {t("common.action.proceed")}
                </Button>,
                <Button key="cancel" onClick={onClose} id="prompt-cancel">
                    {t("common.action.cancel")}
                </Button>,
            ]}
        >
            <p>{t("Metadata.unsavedMetaDataWarning")}</p>
        </SimpleDialog>
    );
};

export default PromptModal;
