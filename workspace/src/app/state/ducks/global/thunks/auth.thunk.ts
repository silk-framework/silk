import uuid from 'uuid/v4';
import { AUTH_ENDPOINT, CLIENT_ID } from "../../../../constants";
import qs from 'qs';
import Store from "store";
import { globalSlice } from "../globalSlice";

const {
    loginSuccess,
    logoutUser
} = globalSlice.actions;

/**
 * @private
 * @desc Redirect to oAuth page for Access token
 */
const requestOAuth2AccessToken = () => {
    const state = uuid();
    const oAuthConfigs = {
        redirect_uri: location.origin,
        client_id: CLIENT_ID,
        response_type: "token",
        state
    };
    Store.set('savedState', state);
    location.href = `${AUTH_ENDPOINT}?${qs.stringify(oAuthConfigs)}`;
};

/**
 * @private
 * @desc Store the access token after redirecting
 */
const storeOAuthAccessToken = (accessToken: string) => {
    Store.set('token', accessToken);
    Store.remove('savedState');
};

/**
 * @private
 * @desc Validate the oAuth previous and next states
 * @param currentState
 */
const validateOauthStates = (currentState: string) => currentState === Store.get('savedState');

/**
 * @desc entry point
 */
export const authorize = () => {
    return dispatch => {
        const { state, access_token } = qs.parse(location.hash.substr(1));

        if (state && !validateOauthStates(state)) {
            dispatch(logout());
            location.hash = '';
            throw new Error(
                `Returned State ${state} does not match sent state`
            );
        }

        if (access_token) {
            storeOAuthAccessToken(access_token);
            dispatch(loginSuccess());
        } else {
            requestOAuth2AccessToken();
        }
    }
};

export const getTokenFromStore = () => Store.get('token') || '';

export const isAuthenticated = () => getTokenFromStore() || false;

export const logout = () => {
    return dispatch => {
        Store.remove('token');
        Store.remove('savedState');
        dispatch(logoutUser());
}
};
