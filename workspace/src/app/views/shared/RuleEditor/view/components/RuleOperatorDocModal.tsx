import { Button, Markdown, SimpleDialog, Spacing } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";

interface RuleOperatorModalProps {
    onClose: () => void;
    isOpen: boolean;
    markdownDescription: string;
    description?: string;
    title: string;
}

const RuleOperatorModal: React.FC<RuleOperatorModalProps> = ({
    onClose,
    isOpen,
    markdownDescription,
    description,
    title,
}) => {
    const [t] = useTranslation();
    return (
        <SimpleDialog
            canEscapeKeyClose
            data-test-id={"show-node-data"}
            size="small"
            title={title}
            isOpen={isOpen}
            onClose={onClose}
            actions={[
                <Button key="cancel" onClick={onClose}>
                    {t("common.action.close")}
                </Button>,
            ]}
        >
            <p>{description}</p>
            <Spacing size="large" />
            <Markdown>{markdownDescription}</Markdown>
        </SimpleDialog>
    );
};

export default RuleOperatorModal;
