import { API_ENDPOINT, HOST } from "../constants";

/**
 * Root path of DI
 * @param path
 */
const rootPath = (path: string) => {
    return HOST + path;
};

const apiPath = (path: string) => {
    return rootPath(API_ENDPOINT + path);
};

/**
 * /api/workspace
 * @param query
 */
export const workspaceApi = (query: string) => {
    return apiPath("/workspace") + query;
};

export const projectApi = (relativePath: String) => {
    return apiPath("/workspace/projects") + relativePath;
};

/**
 * /api/workspace
 * @param query
 */
export const coreApi = (query: string) => {
    return apiPath("/core") + query;
};

/**
 * @TODO: will remove when old endpoints move to new endpoint
 * @param query
 */
export const legacyApiEndpoint = (query: string) => {
    return rootPath("/workspace") + query;
};
