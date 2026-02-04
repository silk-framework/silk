import React from "react";
import { StringPreviewContentBlobToggler, InlineText, utils } from "@eccenca/gui-elements";

interface TextTogglerProps {
    text: string | React.ReactNode;
    toggleLength?: number;
}
export const TextToggler = ({ text, toggleLength = 64 }: TextTogglerProps) => {
    console.log(text);
    let previewText = typeof text === "string" ? text : utils.reduceToText(text);
    React.useEffect(() => {
        previewText = typeof text === "string" ? text : utils.reduceToText(text);
    }, [text]);

    return (
        <StringPreviewContentBlobToggler
            content={previewText}
            fullviewContent={<InlineText>{text}</InlineText>}
            previewMaxLength={toggleLength}
            toggleExtendText={"more"}
            toggleReduceText={"less"}
            forceInline
        />
    );
};

export default TextToggler;
