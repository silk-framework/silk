import * as types from "./types";
import { createAction } from "@reduxjs/toolkit";

export const setLocale = createAction(types.CHANGE_LANGUAGE, locale => ({
    payload: {
        locale
    }
}));

export const loginSuccess = createAction(types.LOGIN_SUCCESS);
export const logOutUser = createAction(types.LOG_OUT);

export const setSearchString = createAction(types.CHANGE_SEARCH_STRING, searchQuery => ({
    payload: {
        searchQuery
    }
}));
