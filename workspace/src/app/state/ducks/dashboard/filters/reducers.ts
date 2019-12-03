import { createReducer } from "@reduxjs/toolkit";
import without from 'ramda/src/without';

import {
    applyFacet,
    applyFilter,
    changePage,
    fetchTypeMod,
    updateFacets,
    updateModifiers,
    updateResultsTotal
} from "./actions";
import { initialPaginationState } from "../../../dto";
import { initialAppliedFacetState, initialFiltersState } from "./dtos";

const dashboardFiltersReducers = createReducer(initialFiltersState(), {
        [fetchTypeMod.type]: (state) => {
            // @Note:  clean all modifiers
            state.modifiers = {}
        },

        [updateModifiers.type]: (state, action) => {
            const {fieldName, modifier} = action.payload;
            state.modifiers[fieldName] = modifier;
        },

        [applyFilter.toString()]: (state, action) => {
            const {field, value} = action.payload;

            if (value === '') {
                delete state.appliedFilters[field];
            } else {
                state.appliedFilters[field] = value;
            }

            state.pagination = initialPaginationState();
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
