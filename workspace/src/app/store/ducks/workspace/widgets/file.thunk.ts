import { workspaceSel } from "@ducks/workspace";
import { getLegacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { previewSlice } from "@ducks/workspace/previewSlice";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";

const {setError} = previewSlice.actions;
const {setFiles} = widgetsSlice.actions;

export const fetchFilesListAsync = () => {
    return async (dispatch, getState) => {
        const projectId = workspaceSel.currentProjectIdSelector(getState());
        const url = getLegacyApiEndpoint(`/projects/${projectId}/resources`);
        try {
            const {data} = await fetch({
                url,
            });
            dispatch(setFiles(data));
        } catch (e) {
            dispatch(setError(e));
        }
    };
};
