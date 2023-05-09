import { coreApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { Variable } from "./typing";

export const getVariables = (project: string): Promise<FetchResponse<{ variables: Variable[] }>> =>
    fetch({
        url: coreApi("/variableTemplate/variables"),
        query: {
            project,
        },
    });

export const createNewVariable = (payload, project: string): Promise<FetchResponse<{ variables: Variable[] }>> =>
    fetch({
        url: coreApi("/variableTemplate/variables"),
        query: {
            project,
        },
        method: "post",
        body: payload,
    });
