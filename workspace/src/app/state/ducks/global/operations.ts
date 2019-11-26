import { batch } from 'react-redux';
import fetch from '../../../providers/fetch';

import { authorize, isAuthenticated, getTokenFromStore, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";
import { fetchSearchResults, searchResultsFailure, searchResultsSuccess, setSearchString } from "./actions";
import { API_ENDPOINT } from "../../../constants";

const globalSearchAsync = (searchString: string) => {
    return dispatch => {
        batch(() => {
            dispatch(setSearchString(searchString));
            dispatch(fetchSearchResults());
        });
        fetch({
            url: API_ENDPOINT + '/searchItems',
            method:'post',
            body: {
                itemType: 'Transformation',
                textQuery: searchString
            }
        }).then(res => {
            dispatch(searchResultsSuccess(res.data));
        }, err => {
            dispatch(searchResultsFailure(err));
        })
    }
};

export default {
    changeLocale,
    isAuthenticated,
    getTokenFromStore,
    authorize,
    logout,
    globalSearchAsync
};
