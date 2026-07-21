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
            className: "diapp-searchtags__tag",
        };
        return searchTag.includes("Replaceable") ? (
            <ArtefactTag key={searchTag} artefactType="replaceable-input" {...tagProps}>
                {tagContent}
            </ArtefactTag>
        ) : (
            <Tag key={searchTag} emphasis="weaker" {...tagProps}>
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
