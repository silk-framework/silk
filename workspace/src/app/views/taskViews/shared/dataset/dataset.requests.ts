import fetch from "../../../../services/fetch";
import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import { FetchResponse } from "../../../../services/fetch/responseInterceptor";

export const clearDataset = async (projectId: string, datasetId: string): Promise<FetchResponse<void>> => {
    return await fetch({
        url: legacyApiEndpoint(`/projects/${projectId}/datasets/${datasetId}/clear`),
        method: "POST",
    });
};
