import React, { useEffect, useState } from "react";
import { IFacetState } from "@ducks/workspace/typings";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import FacetItem from "./FacetItem";
import { Button, ClassNames, Spacing, TitleSubsection, Label } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router";
import { AppDispatch } from "store/configureStore";

/** List of filter facets used to re-fine search results. */
export default function FacetsList({ projectId }: { projectId?: string }) {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const location = useLocation();
    const locationParams = new URLSearchParams(location.search?.substring(1));

    const [visibleFacetsKeywords, setVisibleFacetsKeywords] = useState({});
    const [toggledFacets, setToggledFacets] = useState<string[]>([]);

    const FACETS_PREVIEW_LIMIT = 7;

    useEffect(() => {
        const visiblesOnly = Object.create(null) as any;
        facets.forEach((facet) => {
            visiblesOnly[facet.id] = [...facet.values]
                .sort((a, b) => Number(isChecked(facet.id, b.id)) - Number(isChecked(facet.id, a.id)))
                .slice(0, FACETS_PREVIEW_LIMIT);
        });
        setVisibleFacetsKeywords(visiblesOnly);
    }, [facets]);

    const isChecked = (facetId: string, value: string): boolean => {
        const existsFacet = appliedFacets.find((o) => o.facetId === facetId);
        if (!existsFacet) {
            return false;
        }
        return existsFacet.keywordIds.includes(value);
    };

    const handleSetFacet = (facet: IFacetState, value: string) => {
        const filterOptions: { [key: string]: string | number } = {
            limit: locationParams.get("limit")!,
            current: locationParams.get("page")!,
        };
        if (projectId) {
            filterOptions.project = projectId;
        }
        dispatch(workspaceOp.toggleFacetOp(facet, value));
        dispatch(workspaceOp.changePageOp(1));
    };

    const toggleShowMore = (facet: IFacetState) => {
        const toggledIndex = toggledFacets.findIndex((f) => f === facet.id);
        const keywords = { ...visibleFacetsKeywords };

        if (toggledIndex > -1) {
            keywords[facet.id] = [...facet.values.slice(0, FACETS_PREVIEW_LIMIT)];
            toggledFacets.splice(toggledIndex, 1);
        } else {
            keywords[facet.id] = [...facet.values];
            toggledFacets.push(facet.id);
        }
        setToggledFacets(toggledFacets);
        setVisibleFacetsKeywords(keywords);
    };

    return (
        <div data-test-id={"search-facets"}>
            {facets.map((facet) => (
                <div className={ClassNames.Typography.NOOVERFLOW} key={facet.id}>
                    <TitleSubsection>
                        <Label
                            isLayoutForElement="h3"
                            text={t(`widget.FacetsList.facet.${facet.id}.label`, facet.label)}
                            tooltip={t(`widget.FacetsList.facet.${facet.id}.description`, facet.description)}
                        />
                    </TitleSubsection>
                    <Spacing size="tiny" />
                    <ul>
                        {visibleFacetsKeywords[facet.id] &&
                            visibleFacetsKeywords[facet.id].map((val) => {
                                const key = `${val.id}-${facet.id}`;
                                return (
                                    <li key={key}>
                                        <FacetItem
                                            data-test-id={"facet-items"}
                                            isChecked={isChecked(facet.id, val.id)}
                                            value={val.id}
                                            onSelectFacet={(valueId) => handleSetFacet(facet, valueId)}
                                            label={
                                                <>
                                                    <span className={ClassNames.Typography.FORCELINEBREAK}>
                                                        {t(
                                                            `widget.FacetsList.facet.${facet.id}.valueLabels.${val.id}`,
                                                            val.label,
                                                        )}
                                                    </span>
                                                    <span> ({val.count})</span>
                                                </>
                                            }
                                        />
                                    </li>
                                );
                            })}
                        {facet.values.length > FACETS_PREVIEW_LIMIT && (
                            <li>
                                <Button
                                    onClick={() => toggleShowMore(facet)}
                                    text={t("common.action.ShowSmth", {
                                        smth: toggledFacets.includes(facet.id)
                                            ? t("common.words.less", "less")
                                            : t("common.words.more"),
                                    })}
                                    rightIcon={
                                        toggledFacets.includes(facet.id) ? "toggler-showless" : "toggler-showmore"
                                    }
                                    small
                                    minimal
                                />
                            </li>
                        )}
                    </ul>
                    <Spacing />
                </div>
            ))}
        </div>
    );
}
