import { coreApi } from "../../../utils/getApiEndpoint";
import fetch from "../../../services/fetch";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";
import { TemplateVariableError, Variable, VariableDependencies } from "./typing";

/**
 * Get variables per project or task
 * @param project
 * @param task  If provided, retrieves task-scope variables instead of project-scope.
 * @returns
 */
export const getVariables = (
    project: string,
    task?: string,
): Promise<FetchResponse<{ variables: Variable[]; errors: TemplateVariableError[] }>> =>
    fetch({
        url: coreApi("/variableTemplate/variables"),
        query: {
            project,
            task,
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
    task?: string,
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
 * @param task  If provided, updates a task-scope variable instead of project-scope.
 * @returns
 */
export const updateVariable = (
    payload,
    project: string,
    name: string,
    task?: string,
): Promise<FetchResponse<any>> =>
    fetch({
        url: coreApi(`/variableTemplate/variables/${name}`),
        query: {
            project,
            task,
        },
        method: "put",
        body: payload,
    });

/**
 * Delete a single variable by name
 * @param project
 * @param name
 * @param task  If provided, deletes from task-scope instead of project-scope.
 * @returns
 */
export const deleteVariableRequest = (project: string, name: string, task?: string) =>
    fetch({
        url: coreApi(`/variableTemplate/variables/${name}`),
        query: {
            project,
            name,
            task,
        },
        method: "delete",
    });

/** reorder variable list by providing the intended order */
export const reorderVariablesRequest = (project: string, reorderedVariables: string[], task?: string) =>
    fetch({
        url: coreApi("/variableTemplate/reorderVariables"),
        query: {
            project,
            task,
        },
        method: "post",
        body: reorderedVariables,
    });

export const getVariableDependencies = (
    project: string,
    variable: string,
    task?: string,
): Promise<FetchResponse<VariableDependencies>> =>
    fetch({
        url: coreApi(`/variableTemplate/variables/${variable}/dependencies`),
        query: { project, task },
        method: "get",
    });
