import { createSlice } from "@reduxjs/toolkit";
import { initialPreviewState } from "./initialState";

export const previewSlice = createSlice({
    name: "preview",
    initialState: initialPreviewState(),
    reducers: {
        setLoading(state, action) {
            state.isLoading = action.payload;
        },
        setError(state, action) {
            state.error = action.payload;
        },
        fetchList(state) {
            state.searchResults.length = 0;
        },
        fetchListSuccess(state, action) {
            state.searchResults = action.payload;
        },
    },
});
