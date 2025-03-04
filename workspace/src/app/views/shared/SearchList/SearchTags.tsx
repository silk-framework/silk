import React from "react";
import { Highlighter, Tag, TagList } from "@eccenca/gui-elements";

interface SearchTagsProps {
    searchTags?: string[];
    searchText?: string;
}

export const searchTagsRenderer = (props: SearchTagsProps): JSX.Element[] => {
    const { searchTags = [], searchText = "" } = props;
    return searchTags.map((searchTag) => (
        <Tag emphasis="weaker" key={searchTag}>
            <Highlighter label={searchTag} searchValue={searchText} />
        </Tag>
    ));
};

/** Displays search tags. */
export const SearchTags = (props: SearchTagsProps) => {
    const searchTagsElements = searchTagsRenderer(props);
    return searchTagsElements.length > 0 ? <TagList className="diapp-searchtags">{searchTagsElements}</TagList> : <></>;
};
