import { requestAutocompleteResults } from "@ducks/shared/requests";
import { FetchResponse } from "../../../../services/fetch";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";

export const getAutocompleteResultsAsync = async (payload): Promise<FetchResponse<IAutocompleteDefaultResponse>> => {
    return requestAutocompleteResults({
        limit: 10000,
        offset: 0,
        ...payload,
    });
};
