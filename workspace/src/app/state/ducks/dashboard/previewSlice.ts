import { createSlice } from "@reduxjs/toolkit";
import { initialPreviewState } from "./initialState";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";

export const previewSlice = createSlice({
    name: 'preview',
    initialState: initialPreviewState(),
    reducers: {
        setLoading(state, action) {
            state.isLoading = action.payload;
        },
        setError(state, action) {
            let error = action.payload;
            if (isNetworkError(error)) {
                error = generateNetworkError(error);
            }
            state.error = error;
        },
        fetchList(state) {
            state.searchResults.length = 0;
        },
        fetchListSuccess(state, action) {
            state.searchResults = action.payload;
        },
        cloneTask(state, action) {

        }
    }
});
