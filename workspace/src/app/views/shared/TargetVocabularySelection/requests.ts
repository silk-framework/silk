import { fetch } from "../../../services/fetch/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { workspaceApi } from "../../../utils/getApiEndpoint";
import { IVocabularyInfoRequestResult } from "./typings";

export const requestGlobalVocabularies = async (): Promise<FetchResponse<IVocabularyInfoRequestResult>> => {
    return fetch({
        url: workspaceApi("/vocabularies"),
    });
};
