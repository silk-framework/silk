import fetch from "../../../../services/fetch";
import {FetchResponse} from "../../../../services/fetch/responseInterceptor";
import {legacyTransformEndpoint} from "../../../../utils/getApiEndpoint";
import {IAutocompleteDefaultResponse} from "@ducks/shared/typings";

/** Request value types*/
export const requestValueTypes = async (): Promise<FetchResponse<IAutocompleteDefaultResponse[]>> => {
    const url = legacyTransformEndpoint(`tasks/doesntMatter/doesntMatter/rule/doesntMatter/completions/valueTypes`);
    return fetch({
        url,
        query: {
            maxResults: 10000
        }
    });
}
