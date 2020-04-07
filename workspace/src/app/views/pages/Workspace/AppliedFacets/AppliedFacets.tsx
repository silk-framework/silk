import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IFacetState } from "@ducks/workspace/typings";
import {
    Tag,
    TagList,
} from "@wrappers/index";


export function AppliedFacets() {
    const dispatch = useDispatch();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const handleFacetRemove = (facet: IFacetState, keywordId: string) => {
        dispatch(workspaceOp.toggleFacetOp(facet, keywordId));
    };

    const facetsList = [];
    appliedFacets.map(appliedFacet => {
        const facet = facets.find(o => o.id === appliedFacet.facetId);
        if (facet) {
            facetsList.push({
                label: facet.label,
                id: facet.id,
                keywords: facet.values.filter(key => appliedFacet.keywordIds.includes(key.id))
            });
        }
    });

    return (
        <>
            {
                facetsList.map(facet =>
                    <TagList key={facet.id} label={facet.label}>
                        {
                            facet.keywords.map(keyword =>
                                <Tag
                                    key={keyword.id}
                                    onRemove={() => handleFacetRemove(facet, keyword.id)}
                                >
                                    {keyword.label}
                                </Tag>
                            )
                        }
                    </TagList>
                )
            }
        </>
    )
}
