import { createSlice } from "@reduxjs/toolkit";
import { initialNewPrefixState, initialWidgetsState} from "./initialState";

export const widgetsSlice = createSlice({
    name: 'widgets',
    initialState: initialWidgetsState(),
    reducers: {
        toggleWidgetLoading(state, action) {
            const widgetName = action.payload;
            state[widgetName].error = {};
            state[widgetName].isLoading = !state[action.payload].isLoading;
        },
        setWidgetError(state, action) {
            const {widgetName, error} = action.payload;
            state[widgetName].error = error;
        },
        setPrefixes(state, action) {
            state.configuration.prefixes = action.payload;
        },
        updateNewPrefix(state, action) {
            const {field, value} = action.payload;
            state.configuration.newPrefix = {
                ...state.configuration.newPrefix,
                [field]: value
            };
        },
        resetNewPrefix(state) {
            state.configuration.newPrefix = initialNewPrefixState();
        },
        setWarnings(state, action) {
            state.warnings.results = action.payload;
        },
        setFiles(state, action) {
            state.files.results = action.payload;
        }
    }
});
