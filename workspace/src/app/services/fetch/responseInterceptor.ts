import axios, { AxiosError, AxiosResponse } from "axios";
import { logError } from "../errorLogger";
import { getStore } from "../../store/configureStore";
import { commonOp } from "@ducks/common";
import { isDevelopment } from "../../constants/path";

/** Successful response. */
export class FetchResponse<T = any> {
    public data: T;
    axiosResponse: AxiosResponse;

    constructor(axiosResponse: AxiosResponse) {
        this.axiosResponse = axiosResponse;
        this.data = axiosResponse.data;
    }
}

export class ErrorResponse {
    title: string;
    detail: string;
    cause: string;

    constructor(title: string, detail: string, cause: string) {
        this.title = title;
        this.detail = detail;
        this.cause = cause;
    }
}

export class FetchError {
    static ResponseErrorType = "responseError";
    static NetworkErrorType = "networkError";

    isHttpError: boolean = true;

    errorDetails: AxiosError;

    errorType: typeof FetchError.ResponseErrorType | typeof FetchError.NetworkErrorType;

    errorResponse: ErrorResponse;

    get httpStatus(): number {
        return this.errorDetails.response?.status ? this.errorDetails.response.status : null;
    }
}

/** Error response. */
export class HttpError extends FetchError {
    constructor(errorDetails: AxiosError) {
        super();

        this.errorDetails = errorDetails;
        this.errorType = FetchError.ResponseErrorType;

        this.errorResponse = this.errorDetails.response.data;
    }
}

export class NetworkError extends FetchError {
    constructor(errorDetails: AxiosError) {
        super();

        this.errorDetails = errorDetails;
        this.errorType = FetchError.NetworkErrorType;

        this.errorResponse = new ErrorResponse(
            "Network Error",
            `Please check your connection or contact with support`,
            errorDetails.config.url
        );
    }
}

export const responseInterceptorOnSuccess = (response: AxiosResponse): any => {
    return new FetchResponse(response);
};

export const responseInterceptorOnError = (error: AxiosError) => {
    if (axios.isCancel(error)) {
        return Promise.reject({
            response: {
                data: error,
            },
        });
    }
    if (error.isAxiosError) {
        // It's network error
        if (!error.response) {
            const errorObj = new NetworkError(error);
            logError(errorObj);
            return Promise.reject(errorObj);
        }

        // UnAuthorized
        if (401 === error.response.status) {
            getStore().dispatch(commonOp.logout());
            return Promise.reject(new HttpError(error));
        }
        return Promise.reject(new HttpError(error));
    } else {
        /**
         * @NOTE: This kind of errors not possible, because axios config created manually
         * But we cover this case, to order to avoid future code changes in `try` block
         */
        if (isDevelopment) {
            console.error("Please review Axios configs, actual Error: ", error);
        }
        logError(error);

        return Promise.reject(error);
    }
};
