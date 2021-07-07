import React from "react";
import { DIErrorFormat, ERROR_HANDLED_SECTIONS } from "@ducks/error/typings";
import { Notification } from "@gui-elements/index";
import { useDispatch, useSelector } from "react-redux";
import errorSelector from "@ducks/error/selectors";
import { registerNewError } from "@ducks/error/errorSlice";

type ErrorHandlerRegisterFuncType = (errMsg: string, errorId: string, cause: Error) => JSX.Element;

interface ErrorHandlerDict {
    register: ErrorHandlerRegisterFuncType;
    getAllErrors: () => Array<DIErrorFormat>;
}

const useErrorHandler = (groupId: ERROR_HANDLED_SECTIONS): ErrorHandlerDict => {
    const error = useSelector(errorSelector);
    const dispatch = useDispatch();

    const register: ErrorHandlerRegisterFuncType = (errMsg, errorId, cause) => {
        const error: DIErrorFormat = {
            message: errMsg,
            timestamp: Date.now(),
            cause,
        };
        //push error to state
        dispatch(registerNewError({ groupId, errorId, error }));
        return <Notification message={errMsg} warning />;
    };

    const getAllErrors = () => {
        return Object.values(error[groupId]);
    };

    console.log("ERROR!!!!!!!!", { error });

    return {
        register,
        getAllErrors,
    };
};

export default useErrorHandler;
