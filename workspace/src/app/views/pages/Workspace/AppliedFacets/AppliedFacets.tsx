import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IFacetState } from "@ducks/workspace/typings";
import { Spacing, Tag, TagList } from "@eccenca/gui-elements";
import { AppDispatch } from "store/configureStore";

/** The currently active search filter facets represented as tags. Clicking a tag removes the facet. */
export function AppliedFacets() {
    const dispatch = useDispatch<AppDispatch>();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const handleFacetRemove = (facet: IFacetState, keywordId: string) => {
        dispatch(workspaceOp.toggleFacetOp(facet, keywordId));
        dispatch(workspaceOp.changePageOp(1));
    };

    const facetsList: IFacetState[] = [];
    appliedFacets.forEach((appliedFacet) => {
        const facet = facets.find((o) => o.id === appliedFacet.facetId);
        if (facet) {
            facetsList.push({
                ...facet,
                values: facet.values.filter((key) => appliedFacet.keywordIds.includes(key.id)),
            });
        }
    });

    return (
        <>
            {facetsList.map((facet) => (
                <TagList key={facet.id} label={facet.label}>
                    {facet.values.map((keyword) => (
                        <Tag key={keyword.id} onRemove={() => handleFacetRemove(facet, keyword.id)}>
                            {keyword.label}
                        </Tag>
                    ))}
                </TagList>
            ))}
            {facetsList.length > 0 && <Spacing size="small" />}
        </>
    );
}
