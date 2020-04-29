import { legacyApiEndpoint, workspaceApi } from "../../../../utils/getApiEndpoint";
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

export const getTaskMetadataAsync = async (itemId: string, parentId?: string): Promise<IMetadata> => {
    let url = legacyApiEndpoint(`/projects/${itemId}`);
    if (parentId) {
        url = legacyApiEndpoint(`/projects/${parentId}/tasks/${itemId}/metadata`);
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
    parentId?: string
): Promise<IMetadata> => {
    let url = workspaceApi(`/projects/${itemId}/metaData`);
    if (parentId) {
        url = legacyApiEndpoint(`/projects/${parentId}/tasks/${itemId}/metadata`);
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
