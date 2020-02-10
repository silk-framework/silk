import { createSlice } from "@reduxjs/toolkit";
import { initialNewPrefixState, initialWidgetsState} from "./initialState";

export const widgetsSlice = createSlice({
    name: 'widgets',
    initialState: initialWidgetsState(),
    reducers: {
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
        }
    }
});
