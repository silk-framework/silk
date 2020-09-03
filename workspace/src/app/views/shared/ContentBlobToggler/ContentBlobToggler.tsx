import React, { useState } from "react";
import { HtmlContentBlock, Spacing } from "@gui-elements/index";

interface IContentBlobTogglerProps {
    className?: string;
    previewMaxLength?: number;
    // currently not supported, when necessary we need to move some parts to the GUI elements library to include style rules there
    previewForceSingleLine?: boolean;
    textToggleExtend?: string;
    textToggleReduce?: string;
    contentPreview: JSX.Element | string;
    contentFullview: JSX.Element | string;
    renderContentFullview?(contenFullview: JSX.Element | string): JSX.Element | string;
    startExtended?: boolean;
    // show toggler even if maximum preview content is equal to full content
    showAlwaysToggler?: boolean;
    [otherProps: string]: any;
}

export function ContentBlobToggler({
    className = "",
    previewMaxLength = -1,
    previewForceSingleLine = false,
    textToggleExtend = "show more",
    textToggleReduce = "show less",
    contentPreview,
    contentFullview,
    renderContentFullview = (content) => {return content},
    startExtended = false,
    showAlwaysToggler = false,
    ...otherProps

}: IContentBlobTogglerProps) {

    const [isExtended, setViewState] = useState(startExtended);
    const handlerToggleView = (event) => {
        event.preventDefault();
        event.stopPropagation();
        setViewState(!isExtended);
    }

    const contentPreviewMinimized = (typeof contentPreview === "string" && previewMaxLength > 0) ? contentPreview.substring(0, previewMaxLength) : contentPreview;

    return (
        <div className={className} {...otherProps}>
            <HtmlContentBlock>
            {
                !isExtended ? (
                    <p>
                        {contentPreviewMinimized}
                        {(showAlwaysToggler || (contentFullview !== contentPreviewMinimized)) ? <>&hellip;</> : null}
                        <Spacing vertical={true} size={'tiny'} />
                        {(showAlwaysToggler || (contentFullview !== contentPreviewMinimized)) ? <a href="#more" onClick={(e) => {handlerToggleView(e)}}>{textToggleExtend}</a> : null}
                    </p>
                ) : (
                    <>
                        {renderContentFullview(contentFullview)}
                        <p>
                            <a href="#less" onClick={(e) => {handlerToggleView(e)}}>{textToggleReduce}</a>
                        </p>
                    </>
                )
            }
            </HtmlContentBlock>
        </div>
    )
}
