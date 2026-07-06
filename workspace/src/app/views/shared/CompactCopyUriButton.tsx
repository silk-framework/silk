import { IconButton, IconButtonProps } from "@eccenca/gui-elements";
import React from "react";
import { useTranslation } from "react-i18next";
import useCopyButton from "../../hooks/useCopyButton";

interface CompactCopyUriButtonProps {
    className?: string;
    dataTestId?: string;
    stopPropagation?: boolean;
    uri: string;
    iconButtonProps?: IconButtonProps;
}

/** Button that is used where URI values are displayed to offer convenient copy-to-clipboard functionality. */
export const CompactCopyUriButton: React.FC<CompactCopyUriButtonProps> = ({
    className,
    dataTestId,
    stopPropagation = true,
    uri,
    iconButtonProps,
}) => {
    const [t] = useTranslation();
    const [copyButton] = useCopyButton([
        {
            text: uri,
            ctaMessage: t("common.words.copied", "Copied"),
            confirmationMessage: t("common.action.copyUriClipboard", "Click to copy URI into clipboard"),
            "data-test-id": dataTestId,
            renderButton: ({ isCopied, copiedLabel, copyLabel, onCopy }) => (
                <IconButton
                    className={className}
                    data-test-id={dataTestId}
                    name={isCopied ? "state-confirmed" : "item-copy"}
                    onClick={(event) => {
                        if (stopPropagation) {
                            event.preventDefault();
                            event.stopPropagation();
                        }
                        void onCopy();
                    }}
                    size="small"
                    text={isCopied ? copiedLabel : copyLabel}
                    tooltipAsTitle
                    {...iconButtonProps}
                />
            ),
        },
    ]);

    return copyButton;
};

export default CompactCopyUriButton;
