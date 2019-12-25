import { createSelector } from "@reduxjs/toolkit";
import { IFiltersState, IPreviewState } from "./typings";
import { IStore } from "../../typings/IStore";

const filtersSelector = (state: IStore): IFiltersState => state.dashboard.filters;
const previewSelector = (state: IStore): IPreviewState => state.dashboard.preview;

const isLoadingSelector = createSelector(
    [previewSelector],
    preview => preview.isLoading
);

const errorSelector = createSelector(
    [previewSelector],
    preview => preview.error
);

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

const appliedFacetsSelector = createSelector(
    [appliedFiltersSelector],
    appliedFilters => appliedFilters.facets
);

const paginationSelector = createSelector(
    [filtersSelector],
    filters => filters.pagination
);

export default {
    appliedFiltersSelector,
    appliedFacetsSelector,
    resultsSelector,
    sortersSelector,
    paginationSelector,
    modifiersSelector,
    facetsSelector,
    errorSelector,
    isLoadingSelector
}
