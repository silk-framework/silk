import React from "react";
import { Button } from "@gui-elements/index";
import { useTranslation } from "react-i18next";
import { copyToClipboard } from "../utils/copyToClipboard";

interface ICopyData {
    /** text content to copy to clipboard**/
    text: string;
    /** default text shown before copy action occurs **/
    ctaMessage?: string;
    /** message that shows when the copy button has been clicked, which notifies the user that indeed the copy has been clicked **/
    confirmationMessage?: string;
    /** external callback, that would be called when the copy action has happened**/
    handler?: (text: string) => void | undefined;
}

const COPY_RESET_TIMEOUT = 1000;

const useCopyButton = (data: Array<ICopyData>, resetTimeout = COPY_RESET_TIMEOUT): JSX.Element[] => {
    const [activeButton, setActiveButton] = React.useState<string | undefined>();
    const [t] = useTranslation();
    let timeoutId;

    React.useEffect(() => {
        if (activeButton) {
            timeoutId = setTimeout(() => setActiveButton(undefined), resetTimeout);
        }
        return () => timeoutId && clearTimeout(timeoutId);
    }, [activeButton]);

    return data.map(({ handler, text, ctaMessage, confirmationMessage }, index) => (
        <Button
            onClick={() => {
                copyToClipboard(text);
                setActiveButton(`${index}`);
                //external callback
                handler && handler(text);
            }}
        >
            {activeButton === `${index}`
                ? ctaMessage || t("common.words.copied", "Copied")
                : confirmationMessage || t("common.action.copyClipboard", "Copy to clipboard")}
        </Button>
    ));
};

export default useCopyButton;
