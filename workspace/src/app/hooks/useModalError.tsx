import React from "react";
import { ErrorResponse, FetchError } from "../services/fetch/responseInterceptor";

interface useModalParams {
    setError: (err: ErrorResponse | any) => void;
}

export const useModalError = ({ setError }: useModalParams) => {
    //reset error state start of component
    React.useEffect(() => {
        setError(undefined);
    }, []);

    return (e: any, modalContextName: string) => {
        const errorWithContextName = (error: any) => () => {
            const errorCopy: Record<string, any> = { ...error };
            errorCopy.detail = `${modalContextName}: \n ${error.asString()}`;
            return errorCopy;
        };
        if (e.isFetchError) {
            setError(errorWithContextName((e as FetchError).errorResponse));
        } else if (e.title === "Network Error") {
            setError(errorWithContextName(e));
        } else if(typeof e.asString === "function" && !!e.status) { // If not a FetchError, seems to be an error response then
            setError(errorWithContextName(e))
        } else {
            console.warn(e);
        }
    };
};
