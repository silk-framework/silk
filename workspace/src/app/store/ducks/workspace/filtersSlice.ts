import { createSlice } from "@reduxjs/toolkit";
import { IAppliedSorterState, SortModifierType } from "./typings";
import { initialPaginationState } from "../../typings";
import { initialAppliedFiltersState, initialFiltersState, initialSortersState } from "./initialState";
import i18n from "../../../../language";

const DEFAULT_SORTER = {
    id: "",
    label: i18n.t("common.sorter.recentlyViewed", "Recently viewed"),
};

export const filtersSlice = createSlice({
    name: "filters",
    initialState: initialFiltersState(),
    reducers: {
        applyFilters(state, action) {
            const { ...filters } = action.payload;
            Object.keys(filters).forEach((field) => {
                const value = action.payload[field];
                if (!value) {
                    delete state.appliedFilters[field];
                } else {
                    state.appliedFilters[field] = value;
                }
            });

            state.appliedFacets = [];
            state.pagination = initialPaginationState({
                limit: state.pagination.limit,
            });
        },

        updateSorters(state, action) {
            state.sorters.list = [DEFAULT_SORTER, ...action.payload];
        },

        applySorter(state, action) {
            const { sortBy, sortOrder } = action.payload;
            let appliedSorter: IAppliedSorterState = {
                sortBy: "",
                sortOrder: "",
            };
            if (sortBy) {
                appliedSorter.sortBy = sortBy;
                appliedSorter.sortOrder = sortOrder;
            }
            console.log("appliedSorter ==>", appliedSorter);

            state.sorters.applied = appliedSorter;
        },

        changePage(state, action) {
            const page = action.payload;
            const offset = (page - 1) * state.pagination.limit;
            state.pagination = {
                ...state.pagination,
                offset,
                current: page,
            };
        },

        changeProjectsLimit(state, action) {
            state.pagination.limit = action.payload;
        },

        updateResultTotal: (state, action) => {
            state.pagination.total = action.payload;
        },

        updateFacets(state, action) {
            state.facets = action.payload;
        },

        applyFacet(state, action) {
            const { facet, keywordIds } = action.payload;

            const currentFacet = state.appliedFacets.find((o) => o.facetId === facet.id);
            // add facet, if doesn't exists
            if (!currentFacet) {
                state.appliedFacets.push({
                    facetId: facet.id,
                    type: facet.type,
                    keywordIds,
                });
            } else {
                // push keywordId if keywordId not found in applied facet
                currentFacet.keywordIds = [...currentFacet.keywordIds, ...keywordIds];
            }
        },

        removeFacet(state, action) {
            const { facet, keywordId } = action.payload;

            const ind = state.appliedFacets.findIndex((fa) => fa.facetId === facet.facetId);
            if (ind > -1) {
                const keywords = state.appliedFacets[ind].keywordIds.filter((kw) => kw !== keywordId);
                // Remove if applied facets is empty
                if (!keywords.length) {
                    state.appliedFacets.splice(ind, 1);
                } else {
                    state.appliedFacets[ind].keywordIds = keywords;
                }
            }
        },

        resetFilters(state) {
            state.appliedFilters = initialAppliedFiltersState();
            state.appliedFacets = [];
        },
    },
});
