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
            const newErrors = [...state.errors];
            newErrors.map((err) => {
                if (err.id === error.id) {
                    return error;
                }
                return err;
            });
            state.errors = newErrors;
        },
    },
});

export const { registerNewError } = errorSlice.actions;
export default errorSlice.reducer;
