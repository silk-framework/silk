import { projectSlice } from './projectSlice';
import { batch } from "react-redux";
import fetch from "../../../services/fetch";
import { getLegacyApiEndpoint } from "../../../utils/getApiEndpoint";

const { setProjectId, setError, setLoading, fetchProject, fetchProjectSuccess } = projectSlice.actions;

const setProjectAsync = (projectId: string) => {
    return async dispatch => {
        batch(() => {
            dispatch(setProjectId(projectId));
            dispatch(setError({}));
            dispatch(setLoading(true));
            dispatch(fetchProject());
        });

        try {
            const res = await fetch({
                url: getLegacyApiEndpoint(`/projects/${projectId}`),
            });

            const {results} = res.data;
            batch(() => {
                // Apply results
                dispatch(fetchProjectSuccess(results));
                dispatch(setLoading(false));
            })
        } catch (e) {
            batch(() => {
                dispatch(setLoading(false));
                dispatch(setError(e.response.data));
            })
        }

    }
};

export default {
    setProjectAsync
};
