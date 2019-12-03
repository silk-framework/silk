import { initialPreviewState } from "./dtos";
import { createReducer } from "@reduxjs/toolkit";
import { fetchList, fetchListFailure, fetchListSuccess } from "./actions";

const dashboardPreviewReducers = createReducer(initialPreviewState(), {
    [fetchList.type]: (state) => {
        state.searchResults.length = 0;
        state.isLoading = true;
    },

    [fetchListSuccess.type]: (state, action) => {
        state.isLoading = false;
        state.searchResults = action.payload.results;
    },

    [fetchListFailure.type]: (state, action) => {
        state.isLoading = false;
        state.error = action.payload.error;
    }
});

export default dashboardPreviewReducers;
