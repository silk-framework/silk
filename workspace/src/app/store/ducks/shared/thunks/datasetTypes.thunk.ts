import { requestDatasetTypes } from "@ducks/shared/requests";
import { IDatasetTypesRequest } from "@ducks/shared/typings";

/** Fetches the types of a dataset. */
export const getDatasetTypesAsync = async (typesData: IDatasetTypesRequest): Promise<string[]> => {
    try {
        const { datasetId, projectId, ...restData } = typesData;
        const data = await requestDatasetTypes(datasetId, projectId, restData);
        return data;
    } catch (e) {
        return e;
    }
};
