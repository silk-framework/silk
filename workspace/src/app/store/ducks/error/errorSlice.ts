import { createSlice } from "@reduxjs/toolkit";
import { initialErrorState } from "./initialState";
import { DIErrorFormat } from "./typings";

type RegisterErrorActionType = {
    payload: {
        error: DIErrorFormat;
    };
};

type ClearErrorsActionType = {
    payload: {
        errorIds: Array<string> | undefined;
    };
};

const errorSlice = createSlice({
    name: "error",
    initialState: initialErrorState(),
    reducers: {
        registerNewError(state, action: RegisterErrorActionType) {
            const { error } = action.payload;
            const newErrorsCopy = [...state.errors];
            const similarErrorIndex = newErrorsCopy.findIndex((err) => err.id === error.id);
            similarErrorIndex > -1 ? (newErrorsCopy[similarErrorIndex] = error) : newErrorsCopy.push(error);
            state.errors = newErrorsCopy;
        },
        clearOneOrMoreErrors(state, action: ClearErrorsActionType) {
            const { errorIds } = action.payload;
            /** if there is no error ids delete all else delete just what is in the error id array  */
            if (errorIds?.length) {
                const errorsCopy = [...state.errors];
                const filteredErrors = errorsCopy.filter((err) => !errorIds.includes(err.id));
                state.errors = filteredErrors;
            } else {
                state.errors = [];
            }
        },
    },
});

export const { registerNewError, clearOneOrMoreErrors } = errorSlice.actions;
export default errorSlice.reducer;
