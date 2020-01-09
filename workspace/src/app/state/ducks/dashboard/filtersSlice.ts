import { createSlice } from "@reduxjs/toolkit";
import { IAppliedSorterState, SortModifierType } from "./typings";
import { initialPaginationState } from "../../typings";
import without from "ramda/src/without";
import { initialAppliedSortersState, initialFiltersState } from "./initialState";

const DEFAULT_SORTER = {
    id: '',
    label: 'Default'
};

export const filtersSlice = createSlice({
    name: 'filters',
    initialState: initialFiltersState(),
    reducers: {
        fetchTypeModifier(state) {
            state.modifiers = {}
        },

        updateModifiers(state, action) {
            const {fieldName, modifier} = action.payload;
            state.modifiers[fieldName] = modifier;
        },

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

            state.appliedFilters.facets = [];
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
            const { sortBy, sortOrder } = action.payload;
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

        updateResultTotal: (state, action) => {
            state.pagination.total = action.payload;
        },

        updateFacets(state, action) {
            state.facets = action.payload;
        },

        applyFacet(state, action) {
            const {facet, keywordIds} = action.payload;
            const {facets} = state.appliedFilters;

            const currentFacet = facets.find(o => o.facetId === facet.id);
            // add facet, if doesn't exists
            if (!currentFacet) {
                facets.push({
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
            const {facets} = state.appliedFilters;

            const ind = facets.findIndex(fa => fa.facetId === facet.facetId);
            if (ind > -1) {
                const keywords = facets[ind].keywordIds.filter(kw => kw !== keywordId);
                // Remove if applied facets is empty
                if (!keywords.length) {
                    facets.splice(ind, 1);
                } else {
                    facets[ind].keywordIds = keywords;
                }
                state.pagination = initialPaginationState();
            }
        }
    }
});
