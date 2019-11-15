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
