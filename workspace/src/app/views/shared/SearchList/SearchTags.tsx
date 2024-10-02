import {Highlighter, Spacing, Tag} from "@eccenca/gui-elements"
import {SpacingProps} from "@eccenca/gui-elements/src/components/Separation/Spacing"
import React from "react";

interface SearchTagsProps {
    searchTags?: string[]
    searchText?: string
    // Adds a tiny spacing between the tags
    withSpacing?: SpacingProps["size"] | "none"
}

/** Displays search tags. */
export const SearchTags = ({searchTags, searchText, withSpacing = "tiny"}: SearchTagsProps) => {
    if (!searchTags || searchTags.length === 0) {
        return null
    }
    return (
        <>
            {searchTags.map(searchTag => (
                <div key={searchTag}>
                    <Tag emphasis="weaker">
                        <Highlighter label={searchTag} searchValue={searchText}/>
                    </Tag>
                    {withSpacing !== "none" ? <Spacing size={withSpacing} vertical/> : null}
                </div>
            ))}
        </>
    );
}
