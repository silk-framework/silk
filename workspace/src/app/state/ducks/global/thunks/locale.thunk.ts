import Store from "store";
import { DEFAULT_LANG } from "../../../../constants";
import { globalSlice } from "../globalSlice";

export const getLocale = () => Store.get('locale') || DEFAULT_LANG;

export const changeLocale = (locale) => {
    return dispatch => {
        Store.set('locale', locale);
        dispatch(globalSlice.actions.changeLanguage(locale));
    }
};
