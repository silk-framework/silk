import { createSlice } from "@reduxjs/toolkit";
import { initialProjectState } from "./initialState";
import { generateNetworkError, isNetworkError } from "../../../services/errorLogger";

export const projectSlice = createSlice({
    name: 'project',
    initialState: initialProjectState(),
    reducers: {
        setLoading(state, action) {
            state.isLoading = action.payload;
        },
        setError(state, action) {
            let error = action.payload;
            if (isNetworkError(error)) {
                error = generateNetworkError(error);
            }
            state.error = error;
        },
        setProjectId: (state, action) => {
            state.id = action.payload;
        },
        fetchProject: (state) => {
            state.data = {};
        },
        fetchProjectSuccess: (state, action) => {
            state.data = action.payload;
        }
    }
});
