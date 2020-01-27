import { getLegacyApiEndpoint } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";

const getTaskMetadataAsync = async (taskId: string, projectId: string) => {
    const url = getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadata`);
    const {data} = await fetch({
        url
    });
    return data;
};

export default {
    getTaskMetadataAsync
}
