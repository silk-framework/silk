import axios, { AxiosError, AxiosPromise, AxiosRequestConfig, AxiosResponse, Method } from "axios";
import { is } from "ramda";
import { getStore } from "../store/configureStore";
import { commonOp } from "../store/ducks/common";
import { logError } from "./errorLogger";

interface IFetchOptions {
    url: string;
    method?: Method;
    body?: any;
    headers?: any;
}

/**
 * @private
 * Contain all pending requests Promises
 */
const _pendingRequests = [];

/**
 * @private
 */
const getDefaultHeaders = () => ({
    Accept: "application/json",
    "Content-Type": "application/json",
    // "Authorization": `Bearer ${globalOp.getTokenFromStore()}`
});

/**
 * @private
 * Axios request interceptor
 */
const requestInterceptor = (config: AxiosRequestConfig) => {
    const cfg = {
        ...config,
    };
    if (config.headers["Content-Type"] === "application/x-www-form-urlencoded" && is(Object, config.data)) {
        const { data } = config;

        const serializedData = [];
        for (const key in data) {
            serializedData.push(key + "=" + encodeURIComponent(data[key]));
        }
        cfg.data = serializedData.join("&");
    }

    return cfg;
};

/**
 * @private
 * Axios error response interceptor
 */
const responseInterceptorOnError = (error: AxiosError) => {
    if (axios.isCancel(error)) {
        return Promise.reject({ response: { data: error } });
    }
    if (error.response?.status && 401 === error.response.status) {
        getStore().dispatch(commonOp.logout());
        return Promise.reject(error);
    } else {
        // Store the logs and ignore the promise
        logError(error);
        return Promise.reject(error);
    }
};

// Add a request interceptor
axios.interceptors.request.use(requestInterceptor);

// Add a response interceptor
axios.interceptors.response.use((response) => response, responseInterceptorOnError);

/**
 * @public
 * @param url
 * @param body   The body of the request. In case of a GET request, these are the query parameters.
 * @param method
 * @param headers
 * @param params the URL parameters to be sent with the request
 *
 */
const fetch = ({ url, body, method = "GET", headers = {} }: IFetchOptions): AxiosPromise => {
    const curToken = axios.CancelToken.source();
    _pendingRequests.push(curToken);

    let config: AxiosRequestConfig = {
        method,
        url,
        cancelToken: curToken.token,
        data: body,
    };

    config.headers = {
        ...getDefaultHeaders(),
        ...headers,
    };

    if (method === "GET") {
        config.params = body;
    }

    return axios(config);
};

/** Successful response. */
export class FetchReponse<T = any> {
    protected axiosResponse: AxiosResponse;
    constructor(axiosResponse: AxiosResponse<T>) {
        this.axiosResponse = axiosResponse;
    }
    data: () => T = () => {
        return this.axiosResponse.data;
    };
}

type ErrorType = "errorResponse" | "networkError" | "unknownError";

export class FetchError {
    isFetchError: boolean = true;
    errorType: ErrorType;
}

/** Error response. */
export class ResponseError extends FetchError {
    protected errorDetails: AxiosError;

    constructor(axiosError: AxiosError) {
        super();
        this.errorDetails = axiosError;
    }
    errorResponse: () => any = () => this.errorDetails.response.data;
    httpStatus: () => number | null = () =>
        this.errorDetails.response?.status ? this.errorDetails.response.status : null;
    errorType: ErrorType = "errorResponse";
}

export class NetworkError extends FetchError {
    protected errorDetails: AxiosError;

    constructor(axiosError: AxiosError) {
        super();
        this.errorDetails = axiosError;
    }
    errorType: ErrorType = "networkError";
}

/** An error that is not based on an error response, e.g. evoked by code bugs etc. */
export class UnknownError extends FetchError {
    protected error: any;

    constructor(exception: any) {
        super();
        this.error = exception;
    }

    errorType: ErrorType = "unknownError";
}

/** Adds error handling to requests and returns either a success object.
 * In case of an error it throws one of the specific error objects of type FetchError. */
export const handleRequest = async <T>(fetchResult: AxiosPromise<T>): Promise<FetchReponse<T>> => {
    try {
        const response = await fetchResult;
        return new FetchReponse(response);
    } catch (e) {
        if (e.isFetchError) {
            throw e;
        } else if (e.isAxiosError) {
            if (!e.response) {
                throw new NetworkError(e);
            }
            throw new ResponseError(e);
        } else {
            throw new UnknownError(e);
        }
    }
};

/**
 * @public
 */
const abortPendingRequest = (): boolean => {
    if (_pendingRequests.length) {
        _pendingRequests.map((req) => req.cancel("HTTP Request aborted"));
        return true;
    }
    return false;
};

export { fetch as default, abortPendingRequest };
