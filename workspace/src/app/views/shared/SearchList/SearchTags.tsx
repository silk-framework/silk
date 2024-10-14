import { Highlighter, Spacing, Tag } from "@eccenca/gui-elements";
import React from "react";

interface SearchTagsProps {
    searchTags?: string[];
    searchText?: string;
}

/** Displays search tags. */
export const SearchTags = ({ searchTags, searchText }: SearchTagsProps) => {
    if (!searchTags || searchTags.length === 0) {
        return null;
    }
    return (
        <>
            {searchTags.map((searchTag) => (
                <div key={searchTag}>
                    <Tag emphasis="weaker">
                        <Highlighter label={searchTag} searchValue={searchText} />
                    </Tag>
                    <Spacing size="tiny" vertical />
                </div>
            ))}
        </>
    );
};
