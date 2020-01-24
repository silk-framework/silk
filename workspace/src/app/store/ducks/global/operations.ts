import { authorize, getTokenFromStore, isAuthenticated, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";
import { globalSlice } from "@ducks/global/globalSlice";
import { batch } from "react-redux";
import { getApiEndpoint } from "../../../utils/getApiEndpoint";
import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";

const {addBreadcrumb, setError, fetchAvailableDTypes, updateAvailableDTypes} = globalSlice.actions;

/**
 * Fetch types modifier
 */
const fetchAvailableDTypesAsync = (id?: string) => {
    return async dispatch => {
        batch(() => {
            dispatch(setError({}));
            dispatch(fetchAvailableDTypes());
        });
        try {
            const url = id ? `/searchConfig/types?projectId=${id}` : `/searchConfig/types`;

            const {data} = await fetch({
                url: getApiEndpoint(url),
            });
            const validModifier = asModifier(data.label, 'itemType', data.values);
            batch(() => {
                dispatch(updateAvailableDTypes({
                    fieldName: 'type',
                    modifier: validModifier
                }));
            });
        } catch (e) {
            dispatch(setError(e.response.data));
        }
    }
};

export default {
    changeLocale,
    isAuthenticated,
    getTokenFromStore,
    authorize,
    logout,
    addBreadcrumb,
    fetchAvailableDTypesAsync
};
