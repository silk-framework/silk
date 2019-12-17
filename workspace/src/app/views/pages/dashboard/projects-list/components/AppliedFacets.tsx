import { Tag } from "@blueprintjs/core";
import React from "react";

interface IProps {
    facetsList: {
        id: string;
        label: string;
        keywords: {
            id: string;
            label: string;
        }[];
    }[];

    onFacetRemove(id: string, keywordId: string);
}

export default function AppliedFacets({facetsList, onFacetRemove}: IProps) {
    return (
        <>
            {
                facetsList.map(facet =>
                    <div
                        key={facet.id}
                        className='tags-group'
                    >
                        <div className='tag-label'>{facet.label}:</div>
                        {
                            facet.keywords.map(keyword =>
                                <Tag
                                    key={keyword.id}
                                    className='tag'
                                    onRemove={() => onFacetRemove(facet.id, keyword.id)}
                                >
                                    {keyword.label}
                                </Tag>
                            )
                        }
                    </div>
                )
            }
        </>
    )
}
