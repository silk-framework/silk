import { API_ENDPOINT, HOST, LEGACY_API_ENDPOINT } from "../constants";

// Root path of DI
export const getRootEndpoint = (path: string) => {
    return HOST + path;
};

// API Endpoint
export const getApiEndpoint = (query: string) => {
    return getRootEndpoint(API_ENDPOINT) + query;
};

/**
 * @TODO: will remove when old endpoints move to new endpoint
 * @param query
 */
export const getLegacyApiEndpoint = (query: string) => {
    return getRootEndpoint(LEGACY_API_ENDPOINT) + query;
};

