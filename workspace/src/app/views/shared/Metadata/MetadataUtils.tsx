import { IMetadata } from "@ducks/shared/typings";
import { Keyword, Keywords } from "@ducks/workspace/typings";
import { Tag, TagList } from "@eccenca/gui-elements";
import { ContentBlobToggler } from "@eccenca/gui-elements";
import qs from "qs";
import React from "react";

import { SERVE_PATH } from "../../../constants/path";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { legacyApiEndpoint, workspaceApi } from "../../../utils/getApiEndpoint";
import { IMetadataExpanded } from "./Metadatatypings";

/**
 * if both the taskId and projectId are available then fetch the EXPANDED metadata for tasks
 * else fetch the EXPANDED metadata for project
 * @param projectId
 * @param taskId
 * @returns
 */
const getExpandedMetaData = async (
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<IMetadataExpanded> | undefined> =>
    projectId && taskId
        ? fetch({ url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadataExpanded`) })
        : fetch({ url: workspaceApi(`/projects/${projectId}/metaDataExpanded`) });

/**
 * if both the taskId and projectId are available then create new tag for tasks
 * else fetch the create new tag for project.
 * @param tags
 * @param projectId
 * @param taskId
 * @returns
 */
export const createNewTag = async (
    tags: Partial<Keyword>[],
    projectId?: string
): Promise<FetchResponse<Keywords> | undefined> =>
    fetch({
        url: workspaceApi(`/projects/${projectId}/tags/createTags`),
        method: "post",
        body: { tags },
    });

export const queryTags = (projectId: string, filter?: string): Promise<FetchResponse<{ tags: Keywords }> | undefined> =>
    fetch({
        url: workspaceApi(`/projects/${projectId}/tags${filter?.length ? `?filter=${filter}` : ""}`),
    });

export const updateMetaData = (
    payload: Partial<{ label: string; description: string; tags: string[] }>,
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<IMetadata>> | null => {
    switch (true) {
        case !!(projectId && taskId):
            return fetch({
                url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadata`),
                method: "put",
                body: payload,
            });
        case !!projectId:
            return fetch({
                url: workspaceApi(`/projects/${projectId}/metaData`),
                method: "put",
                body: payload,
            });
        default:
            return null;
    }
};

const DisplayArtefactTags = (
    tags: Keywords,
    t: (key: string, fallBack: string) => string,
    goToPage: (path: string) => void,
    minLength = 6
) => {
    const Tags = (size: "full" | "preview") => (
        <TagList>
            {sortTags(tags)
                .slice(0, size === "full" ? tags.length : minLength)
                .map((tag) => (
                    <Tag key={tag.uri} onClick={() => goToPage(generateFacetUrl("tags", tag.uri))}>
                        {tag.label}
                    </Tag>
                ))}
        </TagList>
    );

    return (
        <>
            {tags.length <= minLength ? (
                Tags("full")
            ) : (
                <ContentBlobToggler
                    previewContent={Tags("preview")}
                    fullviewContent={Tags("full")}
                    toggleExtendText={t("common.words.more", "more")}
                    toggleReduceText={t("common.words.less", "less")}
                />
            )}
        </>
    );
};

const sortTags = (tags: Keywords) =>
    tags.sort((a, b) =>
        a.label.toLowerCase() > b.label.toLowerCase() ? 1 : a.label.toLowerCase() < b.label.toLowerCase() ? -1 : 0
    );

const generateFacetUrl = (id: string, uri: string): string => {
    const queryParams = {
        f_ids: id,
        types: "keyword",
        f_keys: uri,
    };
    return `${SERVE_PATH}?${qs.stringify(queryParams)}`;
};

const getSelectedTagsAndCreateNew = async (
    createdTags: Partial<Keyword>[] = [],
    projectId: string | undefined,
    selectedTags: Keywords = []
) => {
    if (createdTags.length) {
        //create new tags if exists
        const createdTagsResponse = await utils.createNewTag(
            createdTags.map((t) => ({ label: t.label })),
            projectId
        );

        //defensive correction to ensure uris match.
        return selectedTags.map((tag) => {
            const newlyCreatedTagMatch = (createdTagsResponse?.data ?? []).find((t) => t.label === tag.label);
            if (newlyCreatedTagMatch) {
                return newlyCreatedTagMatch.uri;
            }
            return tag.uri;
        });
    }
    return selectedTags.map((tag) => tag.uri);
};

const utils = {
    getExpandedMetaData,
    DisplayArtefactTags,
    createNewTag,
    updateMetaData,
    generateFacetUrl,
    queryTags,
    sortTags,
    getSelectedTagsAndCreateNew,
};

export default utils;
