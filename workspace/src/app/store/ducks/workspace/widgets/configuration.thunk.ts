import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { batch } from "react-redux";
import { commonSel } from "@ducks/common";
import { requestChangePrefixes, requestProjectPrefixes, requestRemoveProjectPrefix } from "@ducks/workspace/requests";

const { setPrefixes, resetNewPrefix, toggleWidgetLoading, setWidgetError } = widgetsSlice.actions;

const WIDGET_NAME = "configuration";

const updatePrefixList = (data) => {
    return (dispatch) => {
        const formattedPrefixes = Object.keys(data).map((key) => ({
            prefixName: key,
            prefixUri: data[key],
        }));
        dispatch(setPrefixes(formattedPrefixes));
    };
};

const toggleLoading = () => (dispatch) => {
    dispatch(toggleWidgetLoading(WIDGET_NAME));
};

const setError = (e) => (dispatch) => {
    dispatch(
        setWidgetError({
            widgetName: WIDGET_NAME,
            error: e,
        })
    );
};

export const fetchProjectPrefixesAsync = () => {
    return async (dispatch, getState) => {
        const projectId = commonSel.currentProjectIdSelector(getState());
        try {
            dispatch(toggleLoading());
            const data = await requestProjectPrefixes(projectId);
            dispatch(updatePrefixList(data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};

export const fetchAddOrUpdatePrefixAsync = (prefixName: string, prefixUri: string) => {
    return async (dispatch, getState) => {
        const projectId = commonSel.currentProjectIdSelector(getState());
        try {
            dispatch(toggleLoading());

            const data = await requestChangePrefixes(prefixName, JSON.stringify(prefixUri), projectId);

            batch(() => {
                dispatch(resetNewPrefix());
                dispatch(updatePrefixList(data));
            });
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};

export const fetchRemoveProjectPrefixAsync = (prefixName: string) => {
    return async (dispatch, getState) => {
        const projectId = commonSel.currentProjectIdSelector(getState());
        try {
            dispatch(toggleLoading());

            const data = await requestRemoveProjectPrefix(prefixName, projectId);

            dispatch(updatePrefixList(data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};
