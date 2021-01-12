import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { HtmlContentBlock, Link } from "@gui-elements/index";

type ContentTransformFunction = (contentPreview: React.ReactNode) => React.ReactNode;
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
    contentPreview: React.ReactNode;
    /**
        content that is displayed as extended full view
    */
    contentFullview: React.ReactNode;
    /**
        render function that could alter full view content, e.g. processing markdown content
    */
    renderContentFullview?: ContentTransformFunction;
    /** render function that could alter the preview content.
     * Default: For string previews it only displays the first non-empty line. */
    renderContentPreview?: ContentTransformFunction;
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

export function ContentBlobToggler({
    className = "",
    previewMaxLength = -1,
    previewForceSingleLine = false,
    textToggleExtend,
    textToggleReduce,
    contentPreview,
    contentFullview,
    renderContentFullview = (content) => {
        return content;
    },
    renderContentPreview,
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

    const [t] = useTranslation();

    const trimmedFullContent = () => (typeof contentFullview === "string" ? contentFullview.trim() : contentFullview);

    const renderedPreviewContent = renderContentPreview ? renderContentPreview(contentPreview) : contentPreview;
    const contentPreviewMinimized =
        typeof renderedPreviewContent === "string" && previewMaxLength > 0
            ? renderedPreviewContent.substr(0, previewMaxLength)
            : renderedPreviewContent;
    const showToggler =
        showAlwaysToggler ||
        (trimmedFullContent() !== contentPreviewMinimized && contentFullview !== contentPreviewMinimized);
    return (
        <div className={className} {...otherProps}>
            <HtmlContentBlock>
                {!isExtended ? (
                    <p>
                        {contentPreviewMinimized}
                        {showToggler ? <>&hellip;</> : null}
                        &nbsp;
                        {showToggler ? (
                            <Link
                                href="#more"
                                onClick={(e) => {
                                    handlerToggleView(e);
                                }}
                            >
                                {!!textToggleExtend ? textToggleExtend : t("common.words.more", "more")}
                            </Link>
                        ) : null}
                    </p>
                ) : (
                    <>
                        {renderContentFullview ? renderContentFullview(contentFullview) : contentFullview}
                        <p>
                            <Link
                                href="#less"
                                onClick={(e) => {
                                    handlerToggleView(e);
                                }}
                            >
                                {!!textToggleReduce ? textToggleReduce : t("common.words.less", "less")}
                            </Link>
                        </p>
                    </>
                )}
            </HtmlContentBlock>
        </div>
    );
}
