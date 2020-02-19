import { workspaceSel } from "@ducks/workspace";
import { getLegacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";

const {setFiles, setWidgetError, toggleWidgetLoading} = widgetsSlice.actions;

const WIDGET_NAME = 'files';

const toggleLoading = () => dispatch => dispatch(toggleWidgetLoading(WIDGET_NAME));

const setError = e => dispatch => dispatch(setWidgetError({
    widgetName: WIDGET_NAME,
    error: e
}));

export const fetchFilesListAsync = () => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getLegacyApiEndpoint(`/projects/${projectId}/resources`);
        try {
            dispatch(toggleLoading());
            const {data} = await fetch({url});
            dispatch(setFiles(data));
            dispatch(toggleLoading());
        } catch (e) {
            dispatch(toggleLoading());
            dispatch(setError(e));
        }
    };
};
