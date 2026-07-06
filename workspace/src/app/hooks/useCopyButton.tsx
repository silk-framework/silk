import React from "react";
import { Button } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { copyToClipboard } from "../utils/copyToClipboard";
import useErrorHandler from "./useErrorHandler";

export interface CopyDataProps {
    /** text content to copy to clipboard**/
    text: string;
    /** default text shown before copy action occurs **/
    ctaMessage?: string;
    /** message that shows when the copy button has been clicked, which notifies the user that indeed the copy has been clicked **/
    confirmationMessage?: string;
    /** external callback, that would be called when the copy action has happened**/
    handler?: (text: string) => void | undefined;
    /** test id for button */
    "data-test-id"?: string;
    renderButton?: (props: CopyButtonRenderProps) => React.JSX.Element;
}

export interface CopyButtonRenderProps {
    copiedLabel: string;
    copyLabel: string;
    isCopied: boolean;
    onCopy: () => Promise<void>;
}

const COPY_RESET_TIMEOUT = 1000;

const useCopyButton = (data: Array<CopyDataProps>, resetTimeout = COPY_RESET_TIMEOUT): React.JSX.Element[] => {
    const [activeButton, setActiveButton] = React.useState<string | undefined>();
    const { registerError } = useErrorHandler();
    const [t] = useTranslation();
    const timeoutRef = React.useRef<number | undefined>(undefined);

    React.useEffect(() => {
        if (activeButton) {
            timeoutRef.current = window.setTimeout(() => setActiveButton(undefined), resetTimeout);
        }
        return () => {
            if (timeoutRef.current != null) {
                window.clearTimeout(timeoutRef.current);
            }
        };
    }, [activeButton, resetTimeout]);

    return data.map(({ handler, text, ctaMessage, confirmationMessage, renderButton, ...rest }, index) => {
        const copyLabel = `${confirmationMessage || t("common.action.copyClipboard", "Copy to clipboard")}`;
        const copiedLabel = `${ctaMessage || t("common.words.copied", "Copied")}`;
        const onCopy = async () => {
            try {
                await copyToClipboard(text);
                setActiveButton(`${index}`);
                handler && handler(text);
            } catch (ex) {
                registerError("useCopyButton", "Could not copy text via copy button.", ex);
            }
        };

        return renderButton ? (
            <React.Fragment key={rest["data-test-id"] ?? `${text}-${index}`}>
                {renderButton({
                    copiedLabel,
                    copyLabel,
                    isCopied: activeButton === `${index}`,
                    onCopy,
                })}
            </React.Fragment>
        ) : (
            <Button
                {...rest}
                key={rest["data-test-id"] ?? `${text}-${index}`}
                onClick={() => {
                    void onCopy();
                }}
            >
                {activeButton === `${index}` ? copiedLabel : copyLabel}
            </Button>
        );
    });
};

export default useCopyButton;
