import React, { useState } from "react";
import { IFacetState } from "@ducks/workspace/typings";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import FacetItem from "./FacetItem";
import { Button, ClassNames, Spacing, TitleSubsection, Label } from "@eccenca/gui-elements";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router";
import { AppDispatch } from "store/configureStore";

export interface FacetsListProps {
    projectId?: string;
    /** Optional display labels for opaque facet IDs; checkbox values remain unchanged. */
    renderValueLabel?: (facetId: string, value: IFacetState["values"][number]) => React.ReactNode;
    prioritizedFacetValues?: Readonly<Record<string, string>>;
}

/** List of filter facets used to re-fine search results. */
export default function FacetsList({ projectId, renderValueLabel, prioritizedFacetValues }: FacetsListProps) {
    const dispatch = useDispatch<AppDispatch>();
    const [t, i18n] = useTranslation();

    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);
    const location = useLocation();
    const locationParams = new URLSearchParams(location.search?.substring(1));

    const [toggledFacets, setToggledFacets] = useState<string[]>([]);

    const FACETS_PREVIEW_LIMIT = 7;

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
        setToggledFacets((current) =>
            current.includes(facet.id) ? current.filter((id) => id !== facet.id) : [...current, facet.id],
        );
    };

    const visibleValues = (facet: IFacetState): IFacetState["values"] => {
        const preferred = prioritizedFacetValues?.[facet.id];
        const values = [...facet.values].sort(
            (a, b) =>
                Number(b.id === preferred) - Number(a.id === preferred) ||
                Number(isChecked(facet.id, b.id)) - Number(isChecked(facet.id, a.id)),
        );
        return toggledFacets.includes(facet.id) ? values : values.slice(0, FACETS_PREVIEW_LIMIT);
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
                        {visibleValues(facet).map((val) => {
                            const key = `${val.id}-${facet.id}`;
                            const translationKey = `widget.FacetsList.facet.${facet.id}.valueLabels.${val.id}`;
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
                                                    {renderValueLabel?.(facet.id, val) ??
                                                        (i18n.exists(translationKey) ? t(translationKey) : val.label)}
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
