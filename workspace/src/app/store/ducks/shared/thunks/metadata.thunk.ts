import { getLegacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";

export const getTaskMetadataAsync = async (itemId: string, parentId?: string) => {
    let url = getLegacyApiEndpoint(`/projects/${itemId}`);
    if (parentId) {
        url = getLegacyApiEndpoint(`/projects/${parentId}/tasks/${itemId}/metadata`);
    }

    try {
        const {data} = await fetch({ url });
        return data;
    } catch(e) {
        return e;
    }
};
