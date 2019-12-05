import { createSlice } from "@reduxjs/toolkit";
import { initialPreviewState } from "./initialState";

export const previewSlice = createSlice({
    name: 'preview',
    initialState: initialPreviewState(),
    reducers: {
        fetchList(state) {
            state.isLoading = true;
            state.searchResults.length = 0;
        },
        fetchListSuccess(state, action) {
            state.isLoading = false;
            state.searchResults = action.payload;
        },
        fetchListFailure(state, action) {
            state.isLoading = false;
            state.error = action.payload;
        },
        cloneTask(state, action) {

        }
    }
});
