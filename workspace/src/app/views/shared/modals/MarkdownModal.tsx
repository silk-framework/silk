import React from "react";
import ReactMarkdown from "react-markdown";
import { Button, SimpleDialog, HtmlContentBlock } from "@gui-elements/index";
import { useTranslation } from "react-i18next";

const MarkdownModal = ({ onDiscard, isOpen, markdown, title = "Error report" }) => {
    const [t] = useTranslation();

    const handleDownload = () => {
        const element = document.createElement("a");
        element.href = window.URL.createObjectURL(new Blob([markdown], { type: "text/markdown" }));
        element.download = `${title}.md`;
        //the above code is equivalent to
        document.body.appendChild(element);
        //onClick property
        element.click();
        document.body.removeChild(element);
    };

    return (
        <SimpleDialog
            title={title}
            isOpen={isOpen}
            onClose={onDiscard}
            actions={[
                <Button affirmative onClick={handleDownload} key="download">
                    {t("common.action.download", "Download")}
                </Button>,
                <Button key="close" onClick={onDiscard}>
                    {t("common.action.close", "Close")}
                </Button>,
            ]}
        >
            <HtmlContentBlock>
                <ReactMarkdown children={markdown} />
            </HtmlContentBlock>
        </SimpleDialog>
    );
};

export default MarkdownModal;
