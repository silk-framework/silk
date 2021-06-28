import { createSlice } from "@reduxjs/toolkit";
import { initialSidebarState } from "./initialState";
import { requestSearchTask } from "./thunks";

export const sidebarSlice = createSlice({
    name: "sidebar",
    initialState: initialSidebarState(),
    reducers: {},
    extraReducers: {
        [requestSearchTask.pending.toString()]: (state) => {
            state.loading = true;
        },
        [requestSearchTask.fulfilled.toString()]: (state, action) => {
            state.loading = false;
            state.results = action.payload.results;
        },
        [requestSearchTask.rejected.toString()]: (state, action) => {
            state.loading = false;
        },
    },
});
