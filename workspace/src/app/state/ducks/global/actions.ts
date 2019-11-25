import * as types from "./types";

export const setLocale = (locale: string) => ({
    type: types.CHANGE_LANGUAGE,
    payload: {
        locale
    }
});

export const loginSuccess = () => ({
    type: types.LOGIN_SUCCESS,
});

export const logOutUser = () => ({
    type: types.LOG_OUT
});

export const setSearchString = (searchString: string) => ({
    type: types.CHANGE_SEARCH_STRING,
    payload: {
        searchString
    }
});

export const fetchSearchResults = () => ({
    type: types.FETCH_SEARCH_RESUTLS,
});

export const searchResultsSuccess = (data) => ({
    type: types.SEARCH_RESUTLS_SUCCESS,
    payload: {
        data,
    }
});

export const searchResultsFailure = (error) => ({
    type: types.SEARCH_RESUTLS_FAILURE,
    payload: {
        error
    }
});
