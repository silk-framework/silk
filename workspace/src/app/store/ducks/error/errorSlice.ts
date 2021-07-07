import { createSlice } from "@reduxjs/toolkit";
import { initialErrorState } from "./initialState";
import { DIErrorFormat, ERROR_HANDLED_SECTIONS } from "./typings";

type RegisterErrorAction = {
    payload: {
        errorId: string;
        groupId: ERROR_HANDLED_SECTIONS;
        error: DIErrorFormat;
    };
};

const errorSlice = createSlice({
    name: "error",
    initialState: initialErrorState(),
    reducers: {
        registerNewError(state, action: RegisterErrorAction) {
            /** payload should contain groupId and error*/
            const { groupId, errorId } = action.payload;
            state[groupId][errorId] = action.payload.error;
        },
    },
});

export const { registerNewError } = errorSlice.actions;
export default errorSlice.reducer;
