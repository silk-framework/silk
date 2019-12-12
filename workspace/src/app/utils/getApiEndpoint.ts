import { API_ENDPOINT, HOST, LEGACY_API_ENDPOINT } from "../constants";

export const getApiEndpoint = (query: string) => {
    return HOST + API_ENDPOINT + query;
};

/**
 * @TODO: will remove when old endpoints move to new endpoint
 * @param query
 */
export const getLegacyApiEndpoint = (query: string) => {
    return HOST + LEGACY_API_ENDPOINT + query;
};
