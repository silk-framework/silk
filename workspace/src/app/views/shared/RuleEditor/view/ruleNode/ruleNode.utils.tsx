import { Highlighter, Spacing, Tag } from "gui-elements";
import React from "react";

/** Adds highlighting to the text if query is non-empty. */
const addHighlighting = (text: string, query?: string): string | JSX.Element => {
    return query ? <Highlighter label={text} searchValue={query} /> : text;
};

/** Creates the tags for an operator (node). */
const createOperatorTags = (tags: string[], query?: string) => {
    return (
        <>
            {tags.map((tag, idx) => {
                return (
                    <>
                        <Tag key={tag} minimal={true}>
                            {addHighlighting(tag, query)}
                        </Tag>
                        {idx < tags.length + 1 ? <Spacing key={`spacing-${tag}`} vertical size="tiny" /> : null}
                    </>
                );
            })}
        </>
    );
};

const ruleNodeUtils = {
    createOperatorTags,
};

export default ruleNodeUtils;
