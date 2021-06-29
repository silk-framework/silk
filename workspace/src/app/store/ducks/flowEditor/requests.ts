import fetch from "../../../services/fetch";
import { workflowApi } from "../../../utils/getApiEndpoint";

const handleError = (e) => {
    return e.errorResponse;
};

interface RequestPortConfigType {
    projectId: string;
    workflowId: string;
}

export const requestConfigPorts = async (payload: RequestPortConfigType) => {
    try {
        const { projectId, workflowId } = payload;
        const { data } = await fetch({
            url: workflowApi(`/config/${projectId}/${workflowId}/ports`),
            method: "GET",
        });
        return data;
    } catch (err) {
        throw handleError(err);
    }
};
