import { createSlice } from "@reduxjs/toolkit";
import { initialMainEditorState } from "./initialState";
import { getConfigPorts } from "./thunks";

export const mainEditorSlice = createSlice({
    name: "editor",
    initialState: initialMainEditorState(),
    reducers: {},
    extraReducers: {
        [getConfigPorts.pending.toString()]: (state) => {
            state.portsConfigLoading = true;
        },
        [getConfigPorts.fulfilled.toString()]: (state, action) => {
            state.portsConfigLoading = false;
            state.portsConfig = action.payload;
        },
        [getConfigPorts.rejected.toString()]: (state, action) => {
            state.portsConfigLoading = false;
        },
    },
});
