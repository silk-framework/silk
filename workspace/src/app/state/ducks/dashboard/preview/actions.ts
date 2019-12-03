import * as types from "./types";
import { createAction } from "@reduxjs/toolkit";

export const fetchList = createAction(types.FETCH_SEARCH_RESULTS);
export const fetchListSuccess = createAction(types.FETCH_SEARCH_RESULTS_SUCCESS, results => ({
    payload: {
        results,
    }
}));

export const fetchListFailure = createAction(types.FETCH_SEARCH_RESULTS_FAILURE, error => ({
    payload: {
        error
    }
}));
