import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { commonSel } from "@ducks/common";
import { requestWarningList, requestWarningMarkdown } from "@ducks/workspace/requests";

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

export const fetchWarningListAsync = () => {
    return async (dispatch, getState) => {
        const projectId = commonSel.currentProjectIdSelector(getState());
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
