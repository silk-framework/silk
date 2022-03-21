import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { requestResourcesList } from "@ducks/shared/requests";

const { setFiles, setWidgetError, toggleWidgetLoading } = widgetsSlice.actions;

const WIDGET_NAME = "files";

const toggleLoading = () => (dispatch) => dispatch(toggleWidgetLoading(WIDGET_NAME));

const setError = (e) => (dispatch) =>
    dispatch(
        setWidgetError({
            widgetName: WIDGET_NAME,
            error: e,
        })
    );

interface IResourceListAsyncProps {
    searchText?: string;
    limit?: number;
    offset?: number;
}

export const fetchResourcesListAsync = (filters: IResourceListAsyncProps = {}, projectId: string) => {
    return async (dispatch) => {
        try {
            dispatch(toggleLoading());
            const response = await requestResourcesList(projectId, filters);
            dispatch(setFiles(response.data));
        } catch (e) {
            dispatch(setError(e));
        } finally {
            dispatch(toggleLoading());
        }
    };
};
