import { coreApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { Variable } from "./typing";

/**
 * Get variables per project
 * @param project
 * @returns
 */
export const getVariables = (project: string): Promise<FetchResponse<{ variables: Variable[] }>> =>
    fetch({
        url: coreApi("/variableTemplate/variables"),
        query: {
            project,
        },
    });

/**
 * Add a new variable or update existing one
 * by modifying existing one and posting the full list of variables here
 * @param payload
 * @param project
 * @param task
 * @returns
 */
export const createNewVariable = (
    payload,
    project: string,
    task?: string
): Promise<FetchResponse<{ variables: Variable[] }>> =>
    fetch({
        url: coreApi("/variableTemplate/variables"),
        query: {
            project,
            task,
        },
        method: "post",
        body: payload,
    });
