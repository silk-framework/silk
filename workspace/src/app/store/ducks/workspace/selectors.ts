import { createSelector } from "@reduxjs/toolkit";
import { IFiltersState, IPreviewState } from "./typings";
import { IStore } from "../../typings/IStore";
import {create} from "domain";

const filtersSelector = (state: IStore): IFiltersState => state.workspace.filters;
const previewSelector = (state: IStore): IPreviewState => state.workspace.preview;

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
    [filtersSelector],
    filters => filters.appliedFacets
);

const paginationSelector = createSelector(
    [filtersSelector],
    filters => filters.pagination
);

const currentProjectIdSelector = createSelector(
    [previewSelector],
    preview => preview.currentProjectId
);

const projectMetadataSelector = createSelector(
    [previewSelector],
    preview => preview.projectMetadata
);

export default {
    appliedFiltersSelector,
    appliedFacetsSelector,
    resultsSelector,
    sortersSelector,
    paginationSelector,
    facetsSelector,
    errorSelector,
    isLoadingSelector,
    currentProjectIdSelector,
    projectMetadataSelector
}
