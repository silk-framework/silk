import React from "react";
import qs from "qs";
import { Tag, TagList } from "gui-elements";

import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { workspaceApi, legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { IMetadataExpanded } from "./Metadatatypings";
import { ContentBlobToggler } from "gui-elements/cmem";
import { SERVE_PATH } from "../../../constants/path";
import { Keyword, Keywords } from "@ducks/workspace/typings";

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

const utils = {
    getExpandedMetaData,
    DisplayArtefactTags,
    createNewTag,
    generateFacetUrl,
    queryTags,
    sortTags,
};

export default utils;
