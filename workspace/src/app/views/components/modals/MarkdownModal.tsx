import { Classes, Intent } from "@wrappers/bluprint/constants";
import React from "react";
import Dialog from "@wrappers/bluprint/dialog";
import ReactMarkdown from "react-markdown";
import Button from "@wrappers/bluprint/button";

const MarkdownModal = ({onDiscard, isOpen, markdown, title = 'Report'}) => {

    const handleDownload = () => {
        const element = document.createElement('a');
        element.href = window.URL.createObjectURL(new Blob([markdown], {type: 'text/markdown'}));
        element.download = `${title}.md`;
        //the above code is equivalent to
        document.body.appendChild(element);
        //onClick property
        element.click();
        document.body.removeChild(element);
    };

    return (
        <Dialog
            icon="info-sign"
            onClose={onDiscard}
            title={title}
            isOpen={isOpen}
            style={{width: 'auto'}}
        >
            <div className={Classes.DIALOG_BODY}>
                <ReactMarkdown source={markdown}/>
            </div>
            <div className={Classes.DIALOG_FOOTER}>
                <div className={Classes.DIALOG_FOOTER_ACTIONS}>
                    <Button
                        intent={Intent.PRIMARY}
                        onClick={handleDownload}
                    >
                        Download
                    </Button>
                    <Button onClick={onDiscard}>Cancel</Button>
                </div>
            </div>
        </Dialog>
    )
};

export default MarkdownModal;
