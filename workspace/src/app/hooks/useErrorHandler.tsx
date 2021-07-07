import React from "react";
import { DIErrorFormat } from "@ducks/error/typings";
import { Notification } from "@gui-elements/index";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { registerNewError } from "@ducks/error/errorSlice";

type ErrorHandlerRegisterFuncType = (errorId: string, errMsg: string, cause: Error) => JSX.Element;

interface ErrorHandlerDict {
    register: ErrorHandlerRegisterFuncType;
    getAllErrors: () => Array<DIErrorFormat>;
}

const useErrorHandler = (): ErrorHandlerDict => {
    const error = useSelector(errorSelector);
    const dispatch = useDispatch();

    const register: ErrorHandlerRegisterFuncType = (errorId, errMsg, cause) => {
        const error: DIErrorFormat = {
            id: errorId,
            message: errMsg,
            timestamp: Date.now(),
            cause,
        };
        //push error to state
        dispatch(registerNewError({ error }));
        return <Notification message={errMsg} warning />;
    };

    const getAllErrors = () => {
        return error.errors;
    };
    return {
        register,
        getAllErrors,
    };
};

export default useErrorHandler;
