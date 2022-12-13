import axios, { AxiosRequestConfig, Method } from "axios";

import { isTestEnv } from "../../constants/path";
import { requestInterceptor } from "./requestInterceptor";
import { FetchResponse, responseInterceptorOnError, responseInterceptorOnSuccess } from "./responseInterceptor";

interface IFetchOptions {
    url: string;
    method?: Method;
    body?: any;
    query?: {
        [key: string]: string | boolean | number | undefined | null;
    };
    headers?: any;
}

// Add a request interceptor
axios.interceptors.request.use(requestInterceptor);

// Add a response interceptor
axios.interceptors.response.use(responseInterceptorOnSuccess, responseInterceptorOnError);

/**
 * @param url     URL of the request
 * @param body    Optional body of the request. In case of a GET request, these are the query parameters.
 * @param method  HTTP method, default: GET
 * @param headers Optional HTTP headers
 * @param params  the URL parameters to be sent with the request
 * @throws FetchError
 */
export const fetch = async <T = any>({
    url,
    body,
    method = "GET",
    headers = {},
    query,
}: IFetchOptions): Promise<FetchResponse<T> | never> => {
    const curToken = axios.CancelToken.source();
    const cToken = curToken.token;

    let config: AxiosRequestConfig = {
        method,
        url,
        cancelToken: cToken,
        data: body,
    };

    config.headers = {
        Accept: "application/json",
        "Content-Type": "application/json",
        ...headers,
    };

    if (method === "GET") {
        config.params = body;
    }
    if (query) {
        config.params = query;
    }
    try {
        if (isTestEnv) {
            config = requestInterceptor(config);
            const response = await axios(config);
            return responseInterceptorOnSuccess(response);
        }
        //@ts-ignore
        return await axios(config);
    } catch (e) {
        if (isTestEnv) {
            return responseInterceptorOnError(e);
        }
        throw e;
    }
};
