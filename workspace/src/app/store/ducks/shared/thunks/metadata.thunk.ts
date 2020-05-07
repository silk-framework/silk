import { legacyApiEndpoint, projectApi, workspaceApi } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { AxiosResponse } from "axios";

interface IRelations {
    inputTasks: [];
    outputTasks: [];
    referencedTasks: [];
    dependentTasksDirect: [];
    dependentTasksAll: [];
}

interface IProjectMetadataResponse {
    name: string;
    description?: string;
    metaData: {
        create: string;
        description: string;
        modified: string;
        label: string;
    };
    tasks: any;
}

interface ITaskMetadataResponse {
    taskType: string;
    schemata: any;
    type?: string;
    modified: string;
    project: string;
    label: string;
    id: string;
    relations: IRelations;
}

export interface IMetadata {
    label: string;
    description: string;
    relations: IRelations;
    type?: string;
}

export interface IMetadataUpdatePayload {
    label: string;
    description?: string;
}

/**
 * Returns the meta data of a specific item.
 * @param itemId    The ID of the item. This also includes project IDs.
 * @param projectId For project items, this parameter must be specified, else the item ID is treated as the project ID.
 */
export const getTaskMetadataAsync = async (itemId: string, projectId?: string): Promise<IMetadata> => {
    let url = projectApi(`/${itemId}/metaData`);
    if (projectId) {
        url = legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`);
    }

    try {
        const { data }: AxiosResponse<IProjectMetadataResponse & ITaskMetadataResponse> = await fetch({ url });
        return {
            label: data.label || (data.metaData ? data.metaData.label : data.name) || data.id,
            description: data.description || (data.metaData ? data.metaData.description : ""),
            relations: data.relations,
            type: data.type || "project",
        };
    } catch (e) {
        return e;
    }
};

export const updateTaskMetadataAsync = async (
    itemId: string,
    payload: IMetadataUpdatePayload,
    projectId?: string
): Promise<IMetadata> => {
    let url = workspaceApi(`/projects/${itemId}/metaData`);
    if (projectId) {
        url = legacyApiEndpoint(`/projects/${projectId}/tasks/${itemId}/metadata`);
    }

    try {
        const { data }: AxiosResponse<IProjectMetadataResponse & ITaskMetadataResponse> = await fetch({
            url,
            method: "PUT",
            body: payload,
        });
        return {
            label: data.label || (data.metaData ? data.metaData.label : data.name) || data.id,
            description: data.description || (data.metaData ? data.metaData.description : ""),
            relations: data.relations,
            type: data.type || "project",
        };
    } catch (e) {
        return e;
    }
};
