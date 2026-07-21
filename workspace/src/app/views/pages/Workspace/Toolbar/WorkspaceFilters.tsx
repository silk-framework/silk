import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { workspaceOp, workspaceSel } from "@ducks/workspace";
import { commonSel } from "@ducks/common";
import { useTranslation } from "react-i18next";
import { IFacetState } from "@ducks/workspace/typings";
import { AppDispatch } from "store/configureStore";
import { createIconNameStack } from "../../../shared/ItemDepiction/ItemDepiction";
import FilterMenu from "./FilterMenu";

/**
 * The workbench filters rendered as inline dropdown "option menus": a single-select item type
 * filter followed by one dropdown per available server facet (e.g. "Created by", tags). Replaces
 * the former left-hand filter sidebar.
 */
export default function WorkspaceFilters({ projectId }: { projectId?: string }) {
    const dispatch = useDispatch<AppDispatch>();
    const [t] = useTranslation();

    const appliedFilters = useSelector(workspaceSel.appliedFiltersSelector);
    const modifiers = useSelector(commonSel.availableDTypesSelector);
    const facets = useSelector(workspaceSel.facetsSelector);
    const appliedFacets = useSelector(workspaceSel.appliedFacetsSelector);

    const typeModifier = modifiers.type;

    const handleTypeSelect = (val: string) => {
        const field = typeModifier.field;
        // Re-selecting the active type clears the filter.
        const value = val !== appliedFilters[field] ? val : "";
        dispatch(workspaceOp.applyFiltersOp({ [field]: value }));
        dispatch(workspaceOp.changePageOp(1));
    };

    const handleFacetToggle = (facet: IFacetState, keywordId: string) => {
        dispatch(workspaceOp.toggleFacetOp(facet, keywordId));
        dispatch(workspaceOp.changePageOp(1));
    };

    const appliedKeywordIds = (facetId: string): string[] =>
        appliedFacets.find((f) => f.facetId === facetId)?.keywordIds ?? [];

    const typeOptions = typeModifier
        ? typeModifier.options
              .filter((mod) => !(!!projectId && (mod.label === "Project" || mod.label === "Global")))
              .map((opt) => ({
                  id: opt.id,
                  label: t("widget.Filterbar.subsections.valueLabels.itemType." + opt.id, opt.label),
                  icon: createIconNameStack(opt.id),
              }))
        : [];

    if (!typeModifier) {
        return null;
    }

    return (
        <div className="flex flex-wrap items-center gap-2" data-test-id="workbench-filters">
            <FilterMenu
                data-test-id="filter-menu-itemType"
                label={t(`widget.Filterbar.subsections.titles.${typeModifier.field}`, typeModifier.label)}
                options={typeOptions}
                selectedIds={appliedFilters[typeModifier.field] ? [appliedFilters[typeModifier.field]] : []}
                onToggle={handleTypeSelect}
            />
            {facets.map((facet) => (
                <FilterMenu
                    key={facet.id}
                    data-test-id={`filter-menu-${facet.id}`}
                    label={t(`widget.FacetsList.facet.${facet.id}.label`, facet.label)}
                    options={facet.values.map((v) => ({
                        id: v.id,
                        label: t(`widget.FacetsList.facet.${facet.id}.valueLabels.${v.id}`, v.label),
                        count: v.count,
                    }))}
                    selectedIds={appliedKeywordIds(facet.id)}
                    onToggle={(keywordId) => handleFacetToggle(facet, keywordId)}
                    // Facets are multi-select: keep the menu open so several keywords can be toggled.
                    closeOnSelect={false}
                />
            ))}
        </div>
    );
}
