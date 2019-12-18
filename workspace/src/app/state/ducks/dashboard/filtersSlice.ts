import { createSlice } from "@reduxjs/toolkit";
import { IAppliedSorterState, SortModifierType } from "./typings";
import { initialPaginationState } from "../../typings";
import without from "ramda/src/without";
import { initialAppliedFacetState, initialFiltersState } from "./initialState";

const DEFAULT_SORTER = {
    id: null,
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

        applyFilter(state, action) {
            const {field, value} = action.payload;
            if (!value) {
                delete state.appliedFilters[field];
            } else {
                state.appliedFilters[field] = value;
                state.sorters.applied = {} as IAppliedSorterState;
                state.appliedFilters.facets = [];
            }

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
            const sortBy = action.payload;
            // clean sorting if default selected
            if (DEFAULT_SORTER.id === sortBy) {
                state.sorters.applied =null;
            } else {
                let sortOrder: SortModifierType = "ASC";
                if (sortBy && currentSort) {
                    sortOrder = currentSort.sortBy === sortBy ? "DESC" : "ASC";
                }
                state.sorters.applied = {
                    sortBy,
                    sortOrder
                };
            }
        },

        changePage(state, action) {
            const page = action.payload;
            const {pagination} = state;
            const offset = (page - 1) * pagination.limit;

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
            const {facet, value} = action.payload;
            const {facets} = state.appliedFilters;

            const ind = facets.findIndex(o => o.facetId === facet.id);
            // add facet, if doesn't not exists
            if (ind === -1) {
                facets.push(
                    initialAppliedFacetState({
                        facetId: facet.id,
                        type: facet.type,
                        keywordIds: [value]
                    }));
                return;
            }
            const currentFacet = facets[ind];

            // push keywordId if keywordId not found in applied facet
            if (!currentFacet.keywordIds.includes(value)) {
                currentFacet.keywordIds.push(value);
                return;
            }

            currentFacet.keywordIds = without([value], currentFacet.keywordIds);
            // remove facet if no any keyword provided
            if (!currentFacet.keywordIds.length) {
                facets.splice(ind, 1);
            }
        },

        removeFacet(state, action) {
            const { facetId, keywordId } = action.payload;
            const { facets } = state.appliedFilters;

            const ind = facets.findIndex(facet => facet.facetId === facetId);
            if (ind > -1) {
                const keywords = facets[ind].keywordIds.filter(kw => kw !== keywordId);
                // Remove if applied facets is empty
                if (!keywords.length) {
                    facets.splice(ind, 1);
                } else {
                    facets[ind].keywordIds = keywords;
                }
            }
        }
    }
});
