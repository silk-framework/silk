import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { requestDetailedProjectPrefixes } from "@ducks/workspace/requests";
import { IDetailedProjectPrefixes } from "@ducks/workspace/typings";

const { setDetailedPrefixes, toggleWidgetLoading, setWidgetError } = widgetsSlice.actions;

const WIDGET_NAME = "configuration";

const formatPrefixMap = (prefixes: Record<string, string>) =>
    Object.keys(prefixes)
        .sort((left, right) => (left < right ? -1 : 1))
        .map((key) => ({
            prefixName: key,
            prefixUri: prefixes[key],
        }));

export const updatePrefixLists = (data: IDetailedProjectPrefixes) => {
    return (dispatch) => {
        const projectPrefixes = formatPrefixMap(data.projectPrefixes);
        const workspacePrefixes = formatPrefixMap(data.workspacePrefixes);
        const prefixes = formatPrefixMap({
            ...data.workspacePrefixes,
            ...data.projectPrefixes,
        });
        dispatch(
            setDetailedPrefixes({
                prefixes,
                projectPrefixes,
                workspacePrefixes,
            }),
        );
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
        }),
    );
};

export const fetchProjectPrefixesAsync = (projectId: string) => {
    return async (dispatch) => {
        try {
            dispatch(toggleLoading());
            const { data } = await requestDetailedProjectPrefixes(projectId);
            dispatch(updatePrefixLists(data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};
