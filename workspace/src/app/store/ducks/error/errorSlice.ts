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
            const newErrorsCopy = [...state.errors];
            const similarErrorIndex = newErrorsCopy.findIndex((err) => err.id === error.id);
            similarErrorIndex > -1 ? (newErrorsCopy[similarErrorIndex] = error) : newErrorsCopy.push(error);
            state.errors = newErrorsCopy;
        },
    },
});

export const { registerNewError } = errorSlice.actions;
export default errorSlice.reducer;
