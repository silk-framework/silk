import React from "react";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { workspaceApi, legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { MetadataExpandedResponse, Tag as TagType } from "./Metadatatypings";
import { ContentBlobToggler } from "gui-elements/cmem";
import { Tag, TagList } from "gui-elements";

/**
 * if both the taskId and projectId are available then fetch the EXPANDED metadata for tasks
 * else fetch the EXPANDED metadata for project.
 * @param projectId
 * @param taskId
 * @returns
 */
const getAllExistingTags = async (
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<MetadataExpandedResponse> | undefined> =>
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
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<Array<TagType>> | undefined> =>
    projectId && taskId
        ? fetch({
              url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/tags/createTags`),
              method: "post",
              body: { tags },
          })
        : fetch({
              url: workspaceApi(`/projects/${projectId}/tags/createTags`),
              method: "post",
              body: { tags },
          });

const DisplayArtefactTags = (tags: Array<TagType>, t: (key: string, fallBack: string) => string) => {
    const Tags = (size: "full" | "preview") => {
        return size === "full" ? (
            <TagList>
                {tags.map((tag) => (
                    <Tag>{tag.label}</Tag>
                ))}
            </TagList>
        ) : (
            <TagList>
                {tags.slice(0, 3).map((tag) => (
                    <Tag>{tag.label}</Tag>
                ))}
            </TagList>
        );
    };
    return (
        <>
            {tags.length <= 3 ? (
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

const utils = {
    getAllExistingTags,
    DisplayArtefactTags,
    createNewTag,
};

export default utils;
