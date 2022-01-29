import React from "react";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import fetch from "../../../services/fetch";
import { workspaceApi, legacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { MetadataExpandedResponse, Tag as TagType } from "./Metadatatypings";
import { ContentBlobToggler } from "gui-elements/cmem";
import { Tag, TagList } from "gui-elements";

//get existing tags for a project or task
const getAllExistingTags = async (
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<MetadataExpandedResponse> | undefined> => {
    if (projectId && taskId) {
        /** fetch tags for task */
        return fetch({ url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadataExpanded`) });
    } else if (projectId && !taskId) {
        /** fetch tags for projects */
        return fetch({ url: workspaceApi(`/projects/${projectId}/metaDataExpanded`) });
    }
};

//create new tags to a project or tasks
export const createNewTag = async (
    tags: Array<Partial<TagType>>,
    projectId?: string,
    taskId?: string
): Promise<FetchResponse<Array<TagType>> | undefined> => {
    if (projectId && taskId) {
        //create tags for tasks
        return fetch({
            url: legacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/tags/createTags`),
            method: "post",
            body: { tags },
        });
    } else if (projectId && !taskId) {
        //create tags for projects
        return fetch({
            url: workspaceApi(`/projects/${projectId}/tags/createTags`),
            method: "post",
            body: { tags },
        });
    }
};

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
