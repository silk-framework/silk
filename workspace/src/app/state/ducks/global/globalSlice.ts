import { createSlice } from "@reduxjs/toolkit";
import { initialGlobalState } from "./initialState";

export const globalSlice = createSlice({
    name: 'global',
    initialState: initialGlobalState(),
    reducers: {
        changeLanguage: (state, action) => {
            state.locale = action.payload.locale;
        },

        loginSuccess: (state) => {
            state.authenticated = true;
        },

        logoutUser: (state) => {
            state.authenticated = false;
        },

        changeSearchString: (state, action) => {
            state.searchQuery = action.payload.searchQuery;
        },
    }
});
