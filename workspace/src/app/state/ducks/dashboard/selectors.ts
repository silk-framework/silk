import { createSelector } from "@reduxjs/toolkit";
import { IFiltersState } from "./typings";
import { IPreviewState } from "./typings/IDashboardPreview";
import { IStore } from "../../typings/IStore";

const filtersSelector = (state: IStore): IFiltersState => state.dashboard.filters;
const previewSelector = (state: IStore): IPreviewState => state.dashboard.preview;

const resultsSelector = createSelector(
    [previewSelector],
    preview => preview.searchResults
);

const modifiersSelector = createSelector(
    [filtersSelector],
    filters => filters.modifiers
);

const sortersSelector = createSelector(
    [filtersSelector],
    filters => filters.sorters
);

const facetsSelector = createSelector(
    [filtersSelector],
    filters => filters.facets
);

const appliedFiltersSelector = createSelector(
    [filtersSelector],
    filters => filters.appliedFilters
);

const paginationSelector = createSelector(
    [filtersSelector],
    filters => filters.pagination
);

export default {
    appliedFiltersSelector,
    resultsSelector,
    sortersSelector,
    paginationSelector,
    modifiersSelector,
    facetsSelector,
}
