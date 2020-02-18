import { workspaceSel } from "@ducks/workspace";
import { getApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { previewSlice } from "@ducks/workspace/previewSlice";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";

const {setError} = previewSlice.actions;
const {setWarnings} = widgetsSlice.actions;

export const fetchWarningListAsync = () => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getApiEndpoint(`/projects/${projectId}/failedTasksReport`);
        try {
            const {data} = await fetch({
                url
            });
            dispatch(setWarnings(data));
        } catch (e) {
            dispatch(setError(e));
        }
    };
};

export const fetchWarningMarkdownAsync = async (projectId: string, taskId: string) => {
    const url = getApiEndpoint(`/projects/${projectId}/failedTasksReport/${taskId}`);
    try {
        const {data} = await fetch({
            url,
            headers: {
                "Accept": "text/markdown",
                'Content-Type': "text/markdown"
            }
        });
        return data;
    } catch {
    }
};
