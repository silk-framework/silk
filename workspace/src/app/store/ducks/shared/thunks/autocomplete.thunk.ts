import { requestAutocompleteResults } from "@ducks/shared/requests";
import { IAutocompleteDefaultResponse, IRequestAutocompletePayload } from "@ducks/shared/typings";
import { FetchResponse } from "../../../../services/fetch/responseInterceptor";

export const getAutocompleteResultsAsync = async (
    payload: IRequestAutocompletePayload
): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    return requestAutocompleteResults({
        limit: 10000,
        offset: 0,
        ...payload,
    });
};
