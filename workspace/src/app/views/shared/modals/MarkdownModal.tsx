import React from "react";
import ReactMarkdown from "react-markdown";
import { Button, SimpleDialog, HtmlContentBlock } from "@wrappers/index";

const MarkdownModal = ({ onDiscard, isOpen, markdown, title = "Error report" }) => {
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
                    Download
                </Button>,
                <Button key="close" onClick={onDiscard}>
                    Close
                </Button>,
            ]}
        >
            <HtmlContentBlock>
                <ReactMarkdown source={markdown} />
            </HtmlContentBlock>
        </SimpleDialog>
    );
};

export default MarkdownModal;
