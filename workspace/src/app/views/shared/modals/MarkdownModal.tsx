import React from "react";
import { Markdown } from "@eccenca/gui-elements";
import { Button, SimpleDialog, HtmlContentBlock } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { SimpleDialogProps } from "@eccenca/gui-elements/src/components/Dialog/SimpleDialog";

interface IProps extends Partial<SimpleDialogProps> {
    onDiscard: () => any;
    isOpen: boolean;
    markdown: string;
    title?: string;
}

/** Render markdown in a modal. */
const MarkdownModal = ({ onDiscard, isOpen, markdown, title = "Error report", ...additionalProps }: IProps) => {
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
            {...additionalProps}
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
                <Markdown>{markdown}</Markdown>
            </HtmlContentBlock>
        </SimpleDialog>
    );
};

export default MarkdownModal;
