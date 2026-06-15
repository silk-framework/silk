import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { initialWidgetsState } from "./initialState";
import { IWidgetsState } from "./typings";

export const widgetsSlice = createSlice({
    name: "widgets",
    initialState: initialWidgetsState(),
    reducers: {
        toggleWidgetLoading(state, action: PayloadAction<keyof IWidgetsState>) {
            const widgetName = action.payload;
            state[widgetName].isLoading = !state[widgetName].isLoading;
        },
        setWidgetError(
            state,
            action: PayloadAction<{
                widgetName: keyof IWidgetsState;
                error: unknown;
            }>,
        ) {
            const { widgetName, error } = action.payload;
            state[widgetName].error = error;
        },
        setWarnings(state, action) {
            state.warnings.results = action.payload;
        },
        setFiles(state, action) {
            state.files.results = action.payload;
        },
    },
});
