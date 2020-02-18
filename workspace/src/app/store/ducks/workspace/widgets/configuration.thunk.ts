import { getApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { workspaceSel } from "@ducks/workspace";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { previewSlice } from "@ducks/workspace/previewSlice";
import { batch } from "react-redux";

const {setPrefixes, resetNewPrefix} = widgetsSlice.actions;
const {setError} = previewSlice.actions;

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

export const fetchProjectPrefixesAsync = () => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes`);
        try {
            const {data} = await fetch({
                url
            });
            dispatch(updatePrefixList(data));
        } catch (e) {
            dispatch(setError(e));
        }
    };
};

export const addOrUpdatePrefixAsync = (prefixName: string, prefixUri: string) => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
        try {
            const {data} = await fetch({
                url,
                method: 'PUT',
                body: JSON.stringify(prefixUri)
            });
            batch(() => {
                dispatch(resetNewPrefix());
                dispatch(updatePrefixList(data));
            })
        } catch (e) {
            dispatch(setError(e));
        }
    }
};

export const removeProjectPrefixAsync = (prefixName: string) => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/prefixes/${prefixName}`);
        try {
            const {data} = await fetch({
                url,
                method: 'DELETE'
            });
            dispatch(updatePrefixList(data));
        } catch (e) {
            dispatch(setError(e));
        }
    }
};
