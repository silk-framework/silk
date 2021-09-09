import { fetch } from "../../../services/fetch/fetch";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import { IVocabularyInfoRequestResult } from "./typings";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

export const requestGlobalVocabularies = async (): Promise<FetchResponse<IVocabularyInfoRequestResult>> => {
    return fetch({
        url: workspaceApi("/vocabularies"),
    });
};
