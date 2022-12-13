import { requestDatasetTypes } from "@ducks/shared/requests";
import { IDatasetTypesRequest } from "@ducks/shared/typings";

import { FetchResponse } from "../../../../services/fetch/responseInterceptor";

/** Fetches the types of a dataset. */
export const getDatasetTypesAsync = async (typesData: IDatasetTypesRequest): Promise<FetchResponse<string[]>> => {
    const { datasetId, projectId, ...restData } = typesData;
    return await requestDatasetTypes(datasetId, projectId, restData);
};
