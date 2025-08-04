import { createSlice } from "@reduxjs/toolkit";
import { initialErrorState } from "./initialState";
import { DIErrorFormat } from "./typings";

type RegisterErrorActionType = {
    payload: {
        /** The error that should be displayed. */
        newError: Pick<DIErrorFormat, "id" | "message" | "cause" | "alternativeIntent">;
        /** An optional error notification instance ID when this error should only be shown in a specific error notification widget. */
        errorNotificationInstanceId?: string;
        /** If true, this error should not automatically open the error queue. */
        notAutoOpen?: boolean;
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
            const { newError, errorNotificationInstanceId, notAutoOpen } = action.payload;
            // Remove old error from the same component action
            const newErrors = state.errors.filter((err) => err.id !== newError.id);
            // Always add new error to the end with current timestamp
            newErrors.push({ ...newError, timestamp: Date.now(), errorNotificationInstanceId, notAutoOpen });
            state.errors = newErrors;
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
