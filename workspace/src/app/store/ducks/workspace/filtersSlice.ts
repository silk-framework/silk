import { createSlice } from "@reduxjs/toolkit";
import { IAppliedSorterState, SortModifierType } from "./typings";
import { initialPaginationState } from "../../typings";
import {
    initialAppliedFiltersState,
    initialAppliedSortersState,
    initialSortersState,
    initialFiltersState
} from "./initialState";

const DEFAULT_SORTER = {
    id: '',
    label: 'Most recently viewed (default)'
};

export const filtersSlice = createSlice({
    name: 'filters',
    initialState: initialFiltersState(),
    reducers: {
        applyFilters(state, action) {
            const filters = action.payload;
            Object.keys(filters).forEach((field) => {
                const value = action.payload[field];
                if (!value) {
                    delete state.appliedFilters[field];
                } else {
                    state.appliedFilters[field] = value;
                    state.sorters.applied = initialAppliedSortersState();
                }
            });

            state.appliedFacets = [];
            state.pagination = initialPaginationState();
        },

        updateSorters(state, action) {
            state.sorters.list = [
                DEFAULT_SORTER,
                ...action.payload
            ]
        },

        applySorter(state, action) {
            const currentSort = state.sorters.applied;
            const {sortBy, sortOrder} = action.payload;
            let appliedSorter: IAppliedSorterState = {
                sortBy: '',
                sortOrder: ''
            };

            if (sortBy) {
                let newSortOrder: SortModifierType = sortOrder || "ASC";
                if (currentSort.sortBy === sortBy) {
                    newSortOrder = currentSort.sortOrder === "ASC" ? "DESC" : "ASC";
                }
                appliedSorter.sortBy = sortBy;
                appliedSorter.sortOrder = newSortOrder;
            }

            state.sorters.applied = appliedSorter;
        },

        changePage(state, action) {
            const page = action.payload;
            const offset = (page - 1) * state.pagination.limit;
            state.pagination = initialPaginationState({
                offset,
                current: page
            });
        },

        changeVisibleProjectsLimit(state, action) {
            state.pagination.limit = action.payload;
        },

        updateResultTotal: (state, action) => {
            state.pagination.total = action.payload;
        },

        updateFacets(state, action) {
            state.facets = action.payload;
        },

        applyFacet(state, action) {
            const {facet, keywordIds} = action.payload;

            const currentFacet = state.appliedFacets.find(o => o.facetId === facet.id);
            // add facet, if doesn't exists
            if (!currentFacet) {
                state.appliedFacets.push({
                    facetId: facet.id,
                    type: facet.type,
                    keywordIds
                });
            } else {
                // push keywordId if keywordId not found in applied facet
                currentFacet.keywordIds = [...currentFacet.keywordIds, ...keywordIds];
            }
            state.pagination = initialPaginationState();
        },

        removeFacet(state, action) {
            const {facet, keywordId} = action.payload;

            const ind = state.appliedFacets.findIndex(fa => fa.facetId === facet.facetId);
            if (ind > -1) {
                const keywords = state.appliedFacets[ind].keywordIds.filter(kw => kw !== keywordId);
                // Remove if applied facets is empty
                if (!keywords.length) {
                    state.appliedFacets.splice(ind, 1);
                } else {
                    state.appliedFacets[ind].keywordIds = keywords;
                }
                state.pagination = initialPaginationState();
            }
        },

        resetFilters(state) {
            state.appliedFilters = initialAppliedFiltersState();
            state.sorters = initialSortersState();
            state.pagination = initialPaginationState();
        }
    }
});
