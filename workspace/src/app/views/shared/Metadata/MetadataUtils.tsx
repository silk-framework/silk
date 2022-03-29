import React from "react";
import qs from "qs";
import { Tag, TagList } from "gui-elements";

import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { workspaceApi, legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { IMetadataExpanded, Tag as TagType } from "./Metadatatypings";
import { ContentBlobToggler } from "gui-elements/cmem";
import { SERVE_PATH } from "../../../constants/path";

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
    tags: Array<Partial<TagType>>,
    projectId?: string
): Promise<FetchResponse<Array<TagType>> | undefined> =>
    fetch({
        url: workspaceApi(`/projects/${projectId}/tags/createTags`),
        method: "post",
        body: { tags },
    });

export const queryTags = (
    projectId: string,
    filter?: string
): Promise<FetchResponse<{ tags: Array<TagType> }> | undefined> =>
    fetch({
        url: workspaceApi(`/projects/${projectId}/tags${filter?.length ? `?filter=${filter}` : ""}`),
    });

const DisplayArtefactTags = (
    tags: Array<TagType>,
    t: (key: string, fallBack: string) => string,
    goToPage: (path: string) => void,
    minLength = 6
) => {
    const Tags = (size: "full" | "preview") => (
        <TagList>
            {tags
                .sort((a, b) =>
                    a.label.toLowerCase() > b.label.toLowerCase()
                        ? -1
                        : a.label.toLowerCase() < b.label.toLowerCase()
                        ? 1
                        : 0
                )
                .slice(0, size === "full" ? tags.length : minLength)
                .map((tag) => (
                    <Tag
                        key={tag.uri}
                        emphasis="stronger"
                        interactive
                        onClick={() => goToPage(generateFacetUrl("tags", tag.uri))}
                    >
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
};

export default utils;
