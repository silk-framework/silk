import React from "react";
import { DIErrorFormat } from "@ducks/error/typings";
import { Notification } from "@gui-elements/index";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { registerNewError, clearOneOrMoreErrors } from "@ducks/error/errorSlice";

type ErrorHandlerRegisterFuncType = (origin: string, errorId: string, errMsg: string, cause: Error) => JSX.Element;

interface ErrorHandlerDict {
    register: ErrorHandlerRegisterFuncType;
    getAllErrors: () => Array<DIErrorFormat>;
    clearErrors: (errorIds?: Array<string> | undefined) => void;
}

const useErrorHandler = (): ErrorHandlerDict => {
    const error = useSelector(errorSelector);
    const dispatch = useDispatch();

    //register a new error to the error stack
    const register: ErrorHandlerRegisterFuncType = (origin, errorId, errMsg, cause) => {
        const error: DIErrorFormat = {
            origin,
            id: errorId,
            message: errMsg,
            timestamp: Date.now(),
            cause,
        };
        //push error to state
        dispatch(registerNewError({ error }));
        return <Notification message={errMsg} warning />;
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
        register,
        getAllErrors,
        clearErrors,
    };
};

export default useErrorHandler;
