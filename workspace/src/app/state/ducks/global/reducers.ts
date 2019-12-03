import * as types from "./types";
import { createReducer } from "@reduxjs/toolkit";
import { initialGlobalState } from "./dtos";


const global = createReducer(initialGlobalState(), {
    [types.CHANGE_LANGUAGE]: (state, action) => {
        state.locale = action.payload.locale;
    },

    [types.LOGIN_SUCCESS]: (state) => {
        state.authenticated = true;
    },

    [types.LOG_OUT]: (state) => {
        state.authenticated = false;
    },

    [types.CHANGE_SEARCH_STRING]: (state, action) => {
        state.searchQuery = action.payload.searchQuery;
    }
});

export default global;
