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

export const projectApi = (relativePath: string) => {
    return apiPath("/workspace/projects") + prependSlash(relativePath);
};

/**
 * /api/workspace
 * @param query
 */
export const coreApi = (query: string) => {
    return apiPath("/core") + query;
};

export const resourcesLegacyApi = (relativePath: string) => {
    return rootPath("/resources") + prependSlash(relativePath);
};

export const datasetsLegacyApi = (relativePath: string) => {
    return rootPath("/datasets") + prependSlash(relativePath);
};

/** In order to build correct paths this function will prepend a slash before the relative path if it's missing. */
const prependSlash = (relativePath: string): string => {
    if (relativePath.startsWith("/") || relativePath.startsWith("?")) {
        return relativePath;
    } else {
        return "/" + relativePath;
    }
};

/**
 * @TODO: will remove when old endpoints move to new endpoint
 * @param query
 */
export const legacyApiEndpoint = (query: string) => {
    return rootPath("/workspace") + query;
};
