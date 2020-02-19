import { getApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { workspaceSel } from "@ducks/workspace";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { batch } from "react-redux";

const {setPrefixes, resetNewPrefix, toggleWidgetLoading, setWidgetError} = widgetsSlice.actions;

const WIDGET_NAME = 'configuration';

const updatePrefixList = (data) => {
    return dispatch => {
        const formattedPrefixes = Object.keys(data)
            .map(key => ({
                prefixName: key,
                prefixUri: data[key]
            }));

        dispatch(setPrefixes(formattedPrefixes));
    }
};

const toggleLoading = () => dispatch => dispatch(toggleWidgetLoading(WIDGET_NAME));

const setError = (e) => dispatch => dispatch(setWidgetError({
    widgetName: WIDGET_NAME,
    error: e
}));

export const fetchProjectPrefixesAsync = () => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes`);
        try {
            dispatch(toggleLoading());
            const {data} = await fetch({url});
            dispatch(updatePrefixList(data));
            dispatch(toggleLoading());
        } catch (e) {
            dispatch(toggleLoading());
            dispatch(setError(e));
        }
    };
};

export const addOrUpdatePrefixAsync = (prefixName: string, prefixUri: string) => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
        try {
            dispatch(toggleLoading());
            const {data} = await fetch({
                url,
                method: 'PUT',
                body: JSON.stringify(prefixUri)
            });
            batch(() => {
                dispatch(resetNewPrefix());
                dispatch(updatePrefixList(data));
                dispatch(toggleLoading());

            })
        } catch (e) {
            dispatch(setError(e));
            dispatch(toggleLoading());
        }
    }
};

export const removeProjectPrefixAsync = (prefixName: string) => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
        try {
            dispatch(toggleLoading());

            const {data} = await fetch({
                url,
                method: 'DELETE'
            });
            dispatch(updatePrefixList(data));
            dispatch(toggleLoading());
        } catch (e) {
            dispatch(setError(e));
            dispatch(toggleLoading());

        }
    }
};
