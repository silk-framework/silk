import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { batch } from "react-redux";
import {
    requestChangePrefixes,
    requestProjectPrefixesLegacy,
    requestRemoveProjectPrefix,
} from "@ducks/workspace/requests";

const { setPrefixes, resetNewPrefix, toggleWidgetLoading, setWidgetError } = widgetsSlice.actions;

const WIDGET_NAME = "configuration";

export const updatePrefixList = (data) => {
    return (dispatch) => {
        const formattedPrefixes = Object.keys(data)
            .sort((left, right) => (left < right ? -1 : 1))
            .map((key) => ({
                prefixName: key,
                prefixUri: data[key],
            }));
        dispatch(setPrefixes(formattedPrefixes));
    };
};

export const toggleLoading = () => (dispatch) => {
    dispatch(toggleWidgetLoading(WIDGET_NAME));
};

export const setError = (e) => (dispatch) => {
    dispatch(
        setWidgetError({
            widgetName: WIDGET_NAME,
            error: e,
        })
    );
};

export const fetchProjectPrefixesAsync = (projectId: string) => {
    return async (dispatch) => {
        try {
            dispatch(toggleLoading());
            const data = await requestProjectPrefixesLegacy(projectId);
            dispatch(updatePrefixList(data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};
