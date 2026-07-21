/**
 * Reduces a markdown string to plain text for single-line contexts such as the table's
 * description cell, where full markdown rendering is undesirable. Handles the constructs that
 * typically appear in item descriptions: emphasis, inline code, links/images (keeping the
 * link resp. alt text), headings, blockquote/list markers and fenced code block delimiters.
 * Not a full markdown parser — unknown syntax is left untouched.
 */
export const stripMarkdown = (markdown: string): string =>
    markdown
        // Fenced code block delimiters (keep the code itself).
        .replace(/^\s*```[^\n]*$/gm, "")
        // Images: keep the alt text.
        .replace(/!\[([^\]]*)\]\([^)]*\)/g, "$1")
        // Links: keep the link text.
        .replace(/\[([^\]]*)\]\([^)]*\)/g, "$1")
        // Inline code: keep the code text.
        .replace(/`([^`]+)`/g, "$1")
        // Bold/italic/strikethrough emphasis markers.
        .replace(/(\*{1,3}|_{1,3}|~~)(\S(?:.*?\S)?)\1/g, "$2")
        // Heading markers at line start.
        .replace(/^#{1,6}\s+/gm, "")
        // Blockquote and (un)ordered list markers at line start.
        .replace(/^\s*(?:>\s?|[-*+]\s+|\d+\.\s+)/gm, "")
        // Collapse line breaks and surrounding whitespace into single spaces.
        .replace(/\s*\n+\s*/g, " ")
        .trim();
