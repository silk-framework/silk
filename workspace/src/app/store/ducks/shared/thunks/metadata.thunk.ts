import { getLegacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";

export const getTaskMetadataAsync = async (taskId: string, projectId: string) => {
    const url = getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadata`);
    try {
        const {data} = await fetch({
            url
        });
        return data;
    } catch {}
};
