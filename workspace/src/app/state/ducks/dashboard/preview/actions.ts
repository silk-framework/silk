import * as types from "./types";

export const fetchList = () => ({
    type: types.FETCH_SEARCH_RESULTS,
});

export const fetchListSuccess = (results) => ({
    type: types.FETCH_SEARCH_RESULTS_SUCCESS,
    payload: {
        results,
    }
});

export const fetchListFailure = (error) => ({
    type: types.FETCH_SEARCH_RESULTS_FAILURE,
    payload: {
        error
    }
});
