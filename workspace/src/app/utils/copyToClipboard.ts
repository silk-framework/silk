/** Copies text to the clipboard.
 * Note that the clipboard API is only available on SSL connections and localhost. */
export const copyToClipboard = (text: string) => {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(text);
    } else {
        // clipboard API not available
        fallbackCopyTextToClipboard(text);
    }
};

/** Fallback copy function taken from https://stackoverflow.com/a/30810322 */
const fallbackCopyTextToClipboard = (text: string) => {
    const textArea = document.createElement("textarea");
    textArea.value = text;

    // Avoid scrolling to bottom
    textArea.style.top = "0";
    textArea.style.left = "0";
    textArea.style.position = "fixed";

    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();

    try {
        document.execCommand("copy");
    } catch (err) {
        throw new Error("Unable to copy text.");
    }

    document.body.removeChild(textArea);
};
