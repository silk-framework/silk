import { ContentBlobToggler } from "./ContentBlobToggler";
import React from "react";

export default ContentBlobToggler;

const newLineRegex = new RegExp("\r|\n"); // eslint-disable-line

/** Takes the first non-empty line from a preview string. */
export function firstNonEmptyLine(preview: React.ReactNode) {
    if (typeof preview === "string") {
        const previewString = preview.trim();
        const result = newLineRegex.exec(previewString);
        return result !== null ? previewString.substr(0, result.index) : previewString;
    } else {
        return preview;
    }
}
