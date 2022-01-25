import { API_ENDPOINT, HOST } from "../constants/path";

/**
 * Root path of DI
 * @param path
 */
export const rootPath = (path: string) => {
    return HOST + path;
};

export const apiPath = (path: string) => {
    return rootPath(API_ENDPOINT + path);
};

/**
 * /api/workspace
 * @param path
 */
export const workspaceApi = (path: string) => {
    return apiPath("/workspace") + prependSlash(path);
};

export const workflowApi = (path: string) => {
    return apiPath("/workflow") + prependSlash(path);
};

export const projectApi = (relativePath: string) => {
    return apiPath("/workspace/projects") + prependSlash(relativePath);
};

/**
 * /api/workspace
 * @param query
 */
export const coreApi = (query: string) => {
    return apiPath("/core") + prependSlash(query);
};

export const resourcesLegacyApi = (relativePath: string) => {
    return rootPath("/resources") + prependSlash(relativePath);
};

/** In order to build correct paths this function will prepend a slash before the relative path if it's missing. */
export const prependSlash = (relativePath: string): string => {
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
    return rootPath("/workspace") + prependSlash(query);
};
