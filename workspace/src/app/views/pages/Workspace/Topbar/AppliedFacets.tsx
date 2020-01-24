import React from "react";
import TagsGroup from "../../../components/tags/TagsGroup";
import TagItem from "../../../components/tags/TagItem";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IFacetState } from "@ducks/workspace/typings";


export default function AppliedFacets() {
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
                    <TagsGroup key={facet.id} label={facet.label}>
                        {
                            facet.keywords.map(keyword =>
                                <TagItem
                                    key={keyword.id}
                                    label={keyword.label}
                                    onFacetRemove={() => handleFacetRemove(facet, keyword.id)}
                                />
                            )
                        }
                    </TagsGroup>
                )
            }
        </>
    )
}
