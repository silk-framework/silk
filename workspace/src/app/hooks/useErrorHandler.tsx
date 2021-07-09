import React from "react";
import { DIErrorFormat, DIErrorTypes } from "@ducks/error/typings";
import { Notification } from "@gui-elements/index";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { registerNewError, clearOneOrMoreErrors } from "@ducks/error/errorSlice";

type ErrorHandlerRegisterFuncType = (errorId: string, errorMessage: string, cause: DIErrorTypes | null) => JSX.Element;

interface ErrorHandlerDict {
    registerError: ErrorHandlerRegisterFuncType;
    getAllErrors: () => Array<DIErrorFormat>;
    clearErrors: (errorIds?: Array<string> | undefined) => void;
}

export type RegisterErrorType = Pick<DIErrorFormat, "id" | "message" | "cause">;

/** Hook for registering errors in the centralized error handling component. */
const useErrorHandler = (): ErrorHandlerDict => {
    const error = useSelector(errorSelector);
    const dispatch = useDispatch();

    /** register a new error to the error stack
     *
     * @param errorId A globally unique error ID that has to be set by the developer. Convention: "<ComponentName>_<ActionName>", e.g. "WorkflowEditor_LoadWorkflow".
     * @param errorMessage Human readable error message.
     * @param cause Optional Error object explaining the exception.
     */
    const registerError: ErrorHandlerRegisterFuncType = (
        errorId: string,
        errorMessage: string,
        cause: DIErrorTypes | null
    ) => {
        const error: RegisterErrorType = {
            id: errorId,
            message: errorMessage,
            cause,
        };
        //push error to state
        dispatch(registerNewError({ newError: error }));
        return <Notification message={errorMessage} warning />;
    };

    // get a list of all errors
    const getAllErrors = () => {
        return error.errors;
    };

    /***
     * deletes all errors corresponding to the ids passed in the parameter,
     * if no parameter is passed it clears all errors.
     *  ***/
    const clearErrors = (errorIds?: Array<string> | undefined) => {
        dispatch(clearOneOrMoreErrors({ errorIds }));
    };

    return {
        registerError,
        getAllErrors,
        clearErrors,
    };
};

export default useErrorHandler;
