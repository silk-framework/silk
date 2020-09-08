import React, { useState } from "react";
import { HtmlContentBlock } from "@gui-elements/index";

interface IContentBlobTogglerProps {
    /**
        space-delimited list of class names
    */
    className?: string;
    /**
        when the preview content is a string then it will be cut to this length
    */
    previewMaxLength?: number;
    // currently not supported, when necessary we need to move some parts to the GUI elements library to include style rules there
    previewForceSingleLine?: boolean;
    /**
        text label used for toggler when preview is displayed
    */
    textToggleExtend?: string;
    /**
        text label used for toggler when full view is displayed
    */
    textToggleReduce?: string;
    /**
        content that is displayed as preview
    */
    contentPreview: JSX.Element | string;
    /**
        content that is displayed as extended full view
    */
    contentFullview: JSX.Element | string;
    /**
        render function that could alter full view content, e.g. processing markdown content
    */
    renderContentFullview?(contenFullview: JSX.Element | string): JSX.Element | string;
    /**
        show extended full view initially
    */
    startExtended?: boolean;
    /**
        show toggler even if maximum preview content is equal to full content
    */
    showAlwaysToggler?: boolean;
    [otherProps: string]: any;
}

const newLineRegex = new RegExp("\r|\n");

export function ContentBlobToggler({
    className = "",
    previewMaxLength = -1,
    previewForceSingleLine = false,
    textToggleExtend = "show more",
    textToggleReduce = "show less",
    contentPreview,
    contentFullview,
    renderContentFullview = (content) => {
        return content;
    },
    startExtended = false,
    showAlwaysToggler = false,
    ...otherProps
}: IContentBlobTogglerProps) {
    const [isExtended, setViewState] = useState(startExtended);
    const handlerToggleView = (event) => {
        event.preventDefault();
        event.stopPropagation();
        setViewState(!isExtended);
    };

    const extractPreviewPart = () => {
        const previewString = (contentPreview as string).trim();
        const result = newLineRegex.exec(previewString);
        let p = previewString;
        if (result !== null) {
            p = previewString.substr(0, result.index);
        }
        return p.substr(0, previewMaxLength);
    };

    const trimmedFullContent = () => (typeof contentFullview === "string" ? contentFullview.trim() : contentFullview);

    const contentPreviewMinimized =
        typeof contentPreview === "string" && (previewMaxLength > 0 || newLineRegex.test(contentPreview))
            ? extractPreviewPart()
            : contentPreview;

    return (
        <div className={className} {...otherProps}>
            <HtmlContentBlock>
                {!isExtended ? (
                    <p>
                        {contentPreviewMinimized}
                        {showAlwaysToggler || trimmedFullContent() !== contentPreviewMinimized ? <>&hellip;</> : null}
                        &nbsp;
                        {showAlwaysToggler || trimmedFullContent() !== contentPreviewMinimized ? (
                            <a
                                href="#more"
                                onClick={(e) => {
                                    handlerToggleView(e);
                                }}
                            >
                                {textToggleExtend}
                            </a>
                        ) : null}
                    </p>
                ) : (
                    <>
                        {renderContentFullview(contentFullview)}
                        <p>
                            <a
                                href="#less"
                                onClick={(e) => {
                                    handlerToggleView(e);
                                }}
                            >
                                {textToggleReduce}
                            </a>
                        </p>
                    </>
                )}
            </HtmlContentBlock>
        </div>
    );
}
