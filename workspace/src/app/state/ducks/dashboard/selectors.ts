import { createSelector } from "reselect";

const filtersSelector = state => state.dashboard.filters;
const previewSelector = state => state.dashboard.preview;

const resultsSelector = createSelector(
    [previewSelector],
    preview => preview.searchResults
);

const modifiersSelector = createSelector(
    [filtersSelector],
    filters => filters.modifiers
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
    paginationSelector,
    modifiersSelector
}
