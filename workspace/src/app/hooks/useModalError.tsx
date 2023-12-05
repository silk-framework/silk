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

    return (e: any) => {
        if (e.isFetchError) {
            setError((e as FetchError).errorResponse);
        } else if (e.title === "Network Error") {
            setError(e);
        } else {
            console.warn(e);
        }
    };
};
