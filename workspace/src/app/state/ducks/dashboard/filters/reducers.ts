import { createReducer } from "@reduxjs/toolkit";
import without from 'ramda/src/without';

import {
    applyFacet,
    applyFilter,
    applySorter,
    changePage,
    fetchTypeMod,
    updateFacets,
    updateModifiers,
    updateResultsTotal, updateSorters
} from "./actions";
import { initialPaginationState } from "../../../dto";
import { IAppliedSorterState, initialAppliedFacetState, initialFiltersState } from "./dtos";

const dashboardFiltersReducers = createReducer(initialFiltersState(), {
        [fetchTypeMod.type]: (state) => {
            state.modifiers = {}
        },

        [updateModifiers.type]: (state, action) => {
            const {fieldName, modifier} = action.payload;
            state.modifiers[fieldName] = modifier;
        },

        [applyFilter.type]: (state, action) => {
            const {field, value} = action.payload;

            if (value === '') {
                delete state.appliedFilters[field];
            } else {
                state.appliedFilters[field] = value;
                state.sorters.applied = {} as IAppliedSorterState;
            }

            state.pagination = initialPaginationState();
        },

        [updateSorters.type]: (state, action) => {
            state.sorters.list = action.payload.sorters;
        },

        [applySorter.type]: (state, action) => {
            const currentSort = state.sorters.applied;
            const {sortBy} = action.payload;
            const sortOrder = sortBy && currentSort === sortBy ? 'DESC' : 'ASC';

            state.sorters.applied = {
                sortBy,
                sortOrder
            };
        },

        [changePage.type]: (state, action) => {
            const {page} = action.payload;
            const {pagination} = state;
            const offset = (page - 1) * pagination.limit + 1;

            state.pagination = initialPaginationState({
                offset,
                current: page
            });
        },

        [updateResultsTotal.type]: (state, action) => {
            state.pagination.total = action.payload.total;
        },

        [updateFacets.type]: (state, action) => {
            state.facets = action.payload.facets;
        },

        [applyFacet.type]: (state, action) => {
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
    })
;

export default dashboardFiltersReducers;
