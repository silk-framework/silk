import { Button, SimpleDialog } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

interface TabSwitchPromptModalProps {
    onClose: () => void;
    isOpen: boolean;
    changeTab: () => void;
}

const TabSwitchPrompt: React.FC<TabSwitchPromptModalProps> = ({ onClose, isOpen, changeTab }) => {
    const [t] = useTranslation();
    return (
        <SimpleDialog
            data-test-id="project-tab-modal"
            title="Warning"
            size="small"
            isOpen={isOpen}
            onClose={onClose}
            intent="warning"
            actions={[
                <Button disruptive={true} onClick={changeTab}>
                    {t("common.action.proceed")}
                </Button>,
                <Button onClick={onClose}>{t("common.action.cancel")}</Button>,
            ]}
        >
            <p>{t("Metadata.unsavedMetaDataWarning")}</p>
        </SimpleDialog>
    );
};

export default TabSwitchPrompt;
