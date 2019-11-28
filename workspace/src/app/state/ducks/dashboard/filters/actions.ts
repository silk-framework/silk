import * as types from "./types";

export const fetchTypeMod = () => ({
    type: types.FETCH_TYPES_MOD,
});

export const fetchTypeModSuccess = (data) => ({
    type: types.FETCH_TYPES_MOD_SUCCESS,
    payload: {
        data,
    }
});

export const fetchTypeModFailure = (error) => ({
    type: types.FETCH_TYPES_MOD_FAILURE,
    payload: {
        error
    }
});

export const fetchAvailableFacets = () => ({
    type: types.FETCH_FACETS,
});

export const fetchAvailableFacetsSuccess = (data) => ({
    type: types.FETCH_FACETS_SUCCESS,
    payload: {
        data,
    }
});

export const fetchAvailableFacetsFailure = (error) => ({
    type: types.FETCH_FACETS_FAILURE,
    payload: {
        error
    }
});

export const applyFilter = (type: string, filterValue: string) => ({
    type: types.APPLY_FILTER,
    payload: {
        type,
        filterValue
    }
});

export const changePage = (page: number) => ({
    type: types.CHANGE_PAGE,
    payload: {
        page,
    }
});

export const updateResultsTotal = (total: number) => ({
    type: types.UPDATE_RESULTS_TOTAL,
    payload: {
        total,
    }
});
