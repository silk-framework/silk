import React from "react";
import { Highlighter, Tag, TagList } from "@eccenca/gui-elements";
import { ArtefactTag } from "../ArtefactTag";

interface SearchTagsProps {
    searchTags?: string[];
    searchText?: string;
}

export const searchTagsRenderer = (props: SearchTagsProps): React.JSX.Element[] => {
    const { searchTags = [], searchText = "" } = props;
    return searchTags.map((searchTag) => {
        const tagContent = <Highlighter label={searchTag} searchValue={searchText} />;
        const tagProps = {
            key: searchTag,
            className: "diapp-searchtags__tag",
        };
        return searchTag.includes("Replaceable") ? (
            <ArtefactTag artefactType="replaceableInput" {...tagProps}>
                {tagContent}
            </ArtefactTag>
        ) : (
            <Tag emphasis="weaker" {...tagProps}>
                {tagContent}
            </Tag>
        );
    });
};

/** Displays search tags. */
export const SearchTags = (props: SearchTagsProps) => {
    const searchTagsElements = searchTagsRenderer(props);
    return searchTagsElements.length > 0 ? <TagList className="diapp-searchtags">{searchTagsElements}</TagList> : <></>;
};
