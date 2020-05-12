import {
    requestProjectMetadata,
    requestTaskMetadata,
    requestUpdateProjectMetadata,
    requestUpdateTaskMetadata,
} from "@ducks/shared/requests";
import { IMetadata, IMetadataUpdatePayload } from "@ducks/shared/typings";

/**
 * Returns the meta data of a specific item.
 * @param itemId    The ID of the item. This also includes project IDs.
 * @param projectId For project items, this parameter must be specified, else the item ID is treated as the project ID.
 */
export const getTaskMetadataAsync = async (itemId: string, projectId?: string): Promise<IMetadata> => {
    try {
        const data = projectId ? await requestTaskMetadata(itemId, projectId) : await requestProjectMetadata(itemId);

        const { label, name, metaData, id, relations, description, type }: any = data;

        return {
            label: label || (metaData ? metaData.label : name) || id,
            description: description || (metaData ? metaData.description : ""),
            relations: relations,
            type: type || "project",
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
    try {
        const data = projectId
            ? await requestUpdateTaskMetadata(itemId, payload, projectId)
            : await requestUpdateProjectMetadata(itemId, payload);

        const { label, name, metaData, id, relations, description, type }: any = data;

        return {
            label: label || (metaData ? metaData.label : name) || id,
            description: description || (metaData ? metaData.description : ""),
            relations: relations,
            type: type || "project",
        };
    } catch (e) {
        return e;
    }
};
