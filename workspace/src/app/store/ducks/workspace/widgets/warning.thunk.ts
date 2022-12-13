import { requestWarningList, requestWarningMarkdown } from "@ducks/workspace/requests";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";

const { setWarnings, setWidgetError, toggleWidgetLoading } = widgetsSlice.actions;

const WIDGET_NAME = "warnings";

const toggleLoading = () => (dispatch) => {
    dispatch(toggleWidgetLoading(WIDGET_NAME));
};

const setError = (e) => (dispatch) =>
    dispatch(
        setWidgetError({
            widgetName: WIDGET_NAME,
            error: e,
        })
    );

export const fetchWarningListAsync = (projectId: string) => {
    return async (dispatch) => {
        dispatch(toggleLoading());
        try {
            const data = await requestWarningList(projectId);
            dispatch(setWarnings(data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};

export const fetchWarningMarkdownAsync = async (taskId: string, projectId: string) => {
    try {
        const data = await requestWarningMarkdown(taskId, projectId);
        return data;
    } catch {}
};
