import {FetchResponse} from "../../../../services/fetch/responseInterceptor";
import {DatasetTaskPlugin} from "@ducks/shared/typings";
import fetch from "../../../../services/fetch";
import {projectApi} from "../../../../utils/getApiEndpoint";

/** Send dataset configuration and get an auto-configured version back. */
export const requestAutoConfiguredDataset = async (projectId: string, dataset: DatasetTaskPlugin<any>): Promise<FetchResponse<DatasetTaskPlugin<any>>> => {
    return fetch({
        url: projectApi(`${projectId}/dataset/autoConfigure  `),
        method: "POST",
        body: dataset
    });
};
