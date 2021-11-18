import {
    requestProjectMetadata,
    requestTaskMetadata,
    requestUpdateProjectMetadata,
    requestUpdateTaskMetadata,
} from "@ducks/shared/requests";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";

/**
 * Returns the meta data of a specific item.
 * @param itemId    The ID of the item. This also includes project IDs. If this parameter is used for project IDs, projectId
 *                  must not be set.
 * @param projectId For project items, this parameter must be specified, else the item ID is treated as the project ID.
 */
export const getTaskMetadataAsync = async (itemId?: string, projectId?: string): Promise<IMetadata> => {
    if (!itemId && !projectId) {
        throw new Error("Either item ID or project ID must be defined in the meta data component.");
    }
    const response =
        projectId && itemId
            ? await requestTaskMetadata(itemId, projectId)
            : await requestProjectMetadata(itemId ? itemId : (projectId as string));

    const { label, name, metaData, id, relations, description, type }: any = response.data;

    return {
        label: label || (metaData ? metaData.label : name) || id,
        description: description || (metaData ? metaData.description : ""),
        relations: relations,
        type: type || "project",
    };
};

export const updateTaskMetadataAsync = async (
    payload: IMetadataUpdatePayload,
    itemId?: string,
    projectId?: string
): Promise<IMetadata> => {
    if (!itemId && !projectId) {
        throw new Error("Either item ID or project ID must be defined in the meta data component.");
    }
    const response =
        projectId && itemId
            ? await requestUpdateTaskMetadata(itemId, payload, projectId)
            : await requestUpdateProjectMetadata(itemId ? itemId : (projectId as string), payload);

    const { label, name, metaData, id, relations, description, type }: any = response.data;

    return {
        label: label || (metaData ? metaData.label : name) || id,
        description: description || (metaData ? metaData.description : ""),
        relations: relations,
        type: type || "project",
    };
};
