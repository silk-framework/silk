import { requestRelatedItems } from "@ducks/shared/requests";

/** Fetches related items for project tasks. */
export const getRelatedItemsAsync = async (projectId: string, taskId: string, textQuery: string = "") => {
    try {
        return await requestRelatedItems(projectId, taskId, textQuery);
    } catch (e) {
        return e;
    }
};
