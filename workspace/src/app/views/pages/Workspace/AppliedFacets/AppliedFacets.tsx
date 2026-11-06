import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { IFacetState } from "@ducks/workspace/typings";
import { Spacing, Tag, TagList } from "@eccenca/gui-elements";
import { AppDispatch } from "store/configureStore";
import { useTranslation } from "react-i18next";

interface AppliedFacetsProps {
    renderTag?: (facetId: string, value: IFacetState["values"][number], onRemove: () => void) => React.ReactNode;
}

/** The active search facets, with a removable tag for each selected value. */
export function AppliedFacets({ renderTag }: AppliedFacetsProps) {
    const dispatch = useDispatch<AppDispatch>();
    const [t, i18n] = useTranslation();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const handleFacetRemove = (facet: IFacetState, keywordId: string) => {
        dispatch(workspaceOp.toggleFacetOp(facet, keywordId));
        dispatch(workspaceOp.changePageOp(1));
    };

    const facetsList: IFacetState[] = appliedFacets
        .filter((appliedFacet) => appliedFacet.keywordIds.length > 0)
        .map((appliedFacet) => {
            const facet = facets.find((candidate) => candidate.id === appliedFacet.facetId);
            return {
                id: appliedFacet.facetId,
                label: facet?.label ?? appliedFacet.facetId,
                description: facet?.description ?? "",
                type: appliedFacet.type,
                // Active values must stay removable even when the current results do not include their metadata.
                values: appliedFacet.keywordIds.map(
                    (id) => facet?.values.find((value) => value.id === id) ?? { id, label: id, count: 0 },
                ),
            };
        });

    const translatedLabel = (key: string, fallback: string): string => (i18n.exists(key) ? t(key) : fallback);

    return (
        <>
            {facetsList.map((facet) => (
                <TagList
                    key={facet.id}
                    data-test-id={`applied-facet-${facet.id}`}
                    label={translatedLabel(`widget.FacetsList.facet.${facet.id}.label`, facet.label)}
                >
                    {facet.values.map((keyword) => {
                        const onRemove = () => handleFacetRemove(facet, keyword.id);
                        return (
                            <React.Fragment key={keyword.id}>
                                {renderTag?.(facet.id, keyword, onRemove) ?? (
                                    <Tag onRemove={onRemove}>
                                        {translatedLabel(
                                            `widget.FacetsList.facet.${facet.id}.valueLabels.${keyword.id}`,
                                            keyword.label,
                                        )}
                                    </Tag>
                                )}
                            </React.Fragment>
                        );
                    })}
                </TagList>
            ))}
            {facetsList.length > 0 && <Spacing size="small" />}
        </>
    );
}
