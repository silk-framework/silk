import { legacyApiEndpoint } from "../../../../utils/getApiEndpoint";
import { AxiosResponse } from "axios";
import fetch from "../../../../services/fetch";

interface IDatasetTypesRequest {
    projectId: string;
    datasetId: string;
    textQuery?: string;
    limit?: number;
}

/** Fetches the types of a dataset. */
export const getDatasetTypesAsync = async (typesData: IDatasetTypesRequest): Promise<string[]> => {
    const url = legacyApiEndpoint(`projects/${typesData.projectId}/datasets/${typesData.datasetId}/types`);
    try {
        const { data }: AxiosResponse<string[]> = await fetch({
            url,
            method: "GET",
            body: {
                textQuery: typesData.textQuery,
                limit: typesData.limit,
            },
        });
        return data;
    } catch (e) {
        return e;
    }
};
