import { createSlice } from "@reduxjs/toolkit";
import { initialErrorState } from "./initialState";
import { DIErrorFormat } from "./typings";

type RegisterErrorAction = {
    payload: {
        error: DIErrorFormat;
    };
};

const errorSlice = createSlice({
    name: "error",
    initialState: initialErrorState(),
    reducers: {
        registerNewError(state, action: RegisterErrorAction) {
            const { error } = action.payload;
            const stateErrorCopy = [...state.errors];
            const newErrors = stateErrorCopy.length
                ? stateErrorCopy.map((err) => (err.id === error.id ? error : err))
                : [error];
            state.errors = newErrors;
        },
    },
});

export const { registerNewError } = errorSlice.actions;
export default errorSlice.reducer;
