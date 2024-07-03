import { createSelector } from "@reduxjs/toolkit";
import { IFiltersState, IPreviewState, IWidgetsState } from "./typings";
import { IStore } from "../../typings/IStore";
import { ICommonState } from "@ducks/common/typings";
import { fileValue } from "@ducks/shared/typings";

const commonSelector = (state: IStore): ICommonState => state.common;
const filtersSelector = (state: IStore): IFiltersState => state.workspace.filters;
const previewSelector = (state: IStore): IPreviewState => state.workspace.preview;
const widgetsSelector = (state: IStore): IWidgetsState => state.workspace.widgets;

const isLoadingSelector = createSelector([previewSelector], (preview) => preview.isLoading);

const errorSelector = createSelector([previewSelector], (preview) => preview.error);

const resultsSelector = createSelector([previewSelector], (preview) => preview.searchResults);

const sortersSelector = createSelector([filtersSelector], (filters) => filters.sorters);

const facetsSelector = createSelector([filtersSelector], (filters) => filters.facets);

const appliedFiltersSelector = createSelector([filtersSelector], (filters) => filters.appliedFilters);

const appliedFacetsSelector = createSelector([filtersSelector], (filters) => filters.appliedFacets);

const paginationSelector = createSelector([filtersSelector], (filters) => filters.pagination);

const prefixListSelector = createSelector([widgetsSelector], (widgets) => widgets.configuration.prefixes);

const widgetErrorSelector = createSelector([widgetsSelector], (widgets) => widgets.configuration.error);

const warningListSelector = createSelector([widgetsSelector], (widgets) => widgets.warnings.results);

const filesListSelector = createSelector([widgetsSelector, commonSelector], (widgets, common) =>
    widgets.files.results.map((item) => ({
        id: fileValue(item),
        formattedDate: item.modified ? new Date(item.modified).toLocaleString() : "N/A",
        formattedSize: item.size ? item.size.toLocaleString(common.locale) : "N/A",
        ...item,
    }))
);

const newPrefixSelector = createSelector([widgetsSelector], (widgets) => widgets.configuration.newPrefix);

const isEmptyPageSelector = createSelector(
    [isLoadingSelector, resultsSelector, commonSelector],
    (isLoading, results, commonStore) => !isLoading && !results.length && commonStore.initialSettings.emptyWorkspace
);

const workspaceSelectors = {
    appliedFiltersSelector,
    appliedFacetsSelector,
    resultsSelector,
    sortersSelector,
    paginationSelector,
    facetsSelector,
    errorSelector,
    isLoadingSelector,
    prefixListSelector,
    newPrefixSelector,
    warningListSelector,
    filesListSelector,
    isEmptyPageSelector,
    widgetsSelector,
    commonSelector,
    widgetErrorSelector,
};

export default workspaceSelectors;
