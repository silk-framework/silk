import axios, { AxiosError, AxiosPromise, AxiosRequestConfig, Method } from 'axios';
import {is} from 'ramda';
import { getStore } from "../state/configureStore";
import { globalOp } from "../state/ducks/global";
import { logError } from "./errorLogger";

interface IFetchOptions {
    url: string;
    method?: Method;
    body?: any;
    headers?: any
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
    "Accept": "application/json",
    // "Authorization": `Bearer ${globalOp.getTokenFromStore()}`
});

/**
 * @private
 * Axios request interceptor
 */
const requestInterceptor = (config: AxiosRequestConfig) => {
    const cfg = {
        ...config
    };
    if (
        config.headers['Content-Type'] === "application/x-www-form-urlencoded" &&
        is(Object, config.data)
    ) {
        const { data } = config;

        const serializedData = [];
        for (const key in data) {
            serializedData.push(key + '=' + encodeURIComponent(data[key]));
        }
        cfg.data = serializedData.join('&');
    }

    return cfg;
};

/**
 * @private
 * Axios error response interceptor
 */
const responseInterceptorOnError = (error: AxiosError) => {
    if (axios.isCancel(error)) {
        return Promise.reject({response: {data: error}})
    }
    if (401 === error.response.status) {
        getStore().dispatch(globalOp.logout());
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
axios.interceptors.response.use(
    response => response,
    responseInterceptorOnError
);

/**
 * @public
 * @param url
 * @param body
 * @param method
 * @param headers
 *
 */
const fetch = ({
    url,
    body,
    method = 'GET',
    headers = {}
}: IFetchOptions): AxiosPromise => {

    const curToken = axios.CancelToken.source();
    _pendingRequests.push(curToken);

    let config: AxiosRequestConfig = {
        method,
        url,
        cancelToken: curToken.token,
        data: body
    };

    config.headers = {
        ...getDefaultHeaders(),
        ...headers
    };

    if (method === 'GET') {
        config.params = body;
    }

    return axios(config);
};

/**
 * @public
 */
const abortPendingRequest = (): boolean => {
    if (_pendingRequests.length) {
        _pendingRequests.map(req => req.cancel('HTTP Request aborted'));
        return true;
    }
    return false;
};

export {
    fetch as default,
    abortPendingRequest
}
