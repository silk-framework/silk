import { API_ENDPOINT, HOST } from "../constants/path";
import { APPLICATION_DOCUMENTATION_SERVICE_URL, APPLICATION_NAME } from "../constants/base";
import { useTranslation } from "react-i18next";
import { commonSel } from "@ducks/common";
import { useSelector } from "react-redux";

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

export const learningApi = (relativePath: string) => {
    return apiPath("/learning") + prependSlash(relativePath);
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
 * @param query
 */
export const legacyApiEndpoint = (query: string) => {
    return rootPath("/workspace") + prependSlash(query);
};

export const legacyTransformEndpoint = (subPath: string) => {
    return rootPath("/transform") + prependSlash(subPath);
};

export const legacyLinkingEndpoint = (subPath: string) => {
    return rootPath("/linking") + prependSlash(subPath);
};

/**
 * Documentation page resolver
 */
export const documentationPageUrl = (featureId: string): string | undefined => {
    const { i18n } = useTranslation();
    const { version } = useSelector(commonSel.initialSettingsSelector);

    try {
        return new URL(
            `?${new URLSearchParams({
                lang: i18n.language,
                version: version ?? "unknown",
                origin: APPLICATION_NAME(),
            }).toString()}`,
            APPLICATION_DOCUMENTATION_SERVICE_URL() + featureId,
        ).toString();
    } catch {
        return undefined;
    }
};
