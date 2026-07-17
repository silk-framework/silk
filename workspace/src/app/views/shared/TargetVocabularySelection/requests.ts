import { fetch } from "../../../services/fetch/fetch";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import { IVocabularyInfoRequestResult, IVocabularyLookupRequest, IVocabularyLookupResponse } from "./typings";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

export const requestGlobalVocabularies = async (): Promise<FetchResponse<IVocabularyInfoRequestResult>> => {
    return fetch({
        url: workspaceApi("/vocabularies"),
    });
};

export const requestVocabularyLookup = async (
    payload: IVocabularyLookupRequest,
): Promise<FetchResponse<IVocabularyLookupResponse>> => {
    return fetch({
        url: workspaceApi("/vocabularies/lookup"),
        method: "POST",
        body: payload,
    });
};
