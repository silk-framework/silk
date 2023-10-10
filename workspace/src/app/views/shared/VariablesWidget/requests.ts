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
 * Add a new variable
 *
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

/**
 * Update a single variable
 * @param project
 * @param name
 * @param payload
 * @returns
 */
export const updateVariable = (payload, project: string, name: string): Promise<FetchResponse<any>> =>
    fetch({
        url: coreApi(`/variableTemplate/variables/${name}`),
        query: {
            project,
        },
        method: "put",
        body: payload,
    });

/**
 * Delete a single variable by name
 * @param project
 * @param name
 * @returns
 */
export const deleteVariableRequest = (project: string, name: string) =>
    fetch({
        url: coreApi(`/variableTemplate/variables/${name}`),
        query: {
            project,
            name,
        },
        method: "delete",
    });

/** reorder variable list by providing the intended order */
export const reorderVariablesRequest = (project, reorderedVariables: string[]) =>
    fetch({
        url: coreApi("/variableTemplate/reorderVariables"),
        query: {
            project,
        },
        method: "post",
        body: reorderedVariables,
    });
