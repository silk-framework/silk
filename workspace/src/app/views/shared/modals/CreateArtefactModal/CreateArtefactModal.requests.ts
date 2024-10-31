import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import { DatasetTaskPlugin } from "@ducks/shared/typings";
import fetch from "../../../../services/fetch";
import { coreApi, projectApi } from "../../../../utils/getApiEndpoint";
import {
    IPartialAutoCompleteResult,
    IValidationResult,
} from "@eccenca/gui-elements/src/components/AutoSuggestion/AutoSuggestion";

/** Send dataset configuration and get an auto-configured version back. */
export const requestAutoConfiguredDataset = async (
    projectId: string,
    dataset: DatasetTaskPlugin<any>
): Promise<FetchResponse<DatasetTaskPlugin<any>>> => {
    return fetch({
        url: projectApi(`${projectId}/dataset/autoConfigure  `),
        method: "POST",
        body: dataset,
    });
};

export interface ValidateTemplateResponse extends IValidationResult {
    /** If the validation was successful, then this is the evaluated string. */
    evaluatedTemplate?: string;
}

/** Validates a variable template. If the validation was successful, the evaluated string is returned. */
export const requestValidateTemplateString = async (
    templateString: string,
    project?: string,
    variableName?: string
): Promise<FetchResponse<ValidateTemplateResponse>> => {
    return fetch({
        url: coreApi("/variableTemplate/validation"),
        method: "POST",
        body: {
            templateString,
            project,
            variableName,
        },
    });
};

/**
 * Auto-complete a variable template.
 * @param inputString
 * @param cursorPosition
 * @param project
 * @param variableName optional parameter to make correct suggestions for when an existing variable is edited
 * @param includeSensitiveVariables include sensitive variables in the autocompletion.
 * @returns
 */
export const requestAutoCompleteTemplateString = async (
    inputString: string,
    cursorPosition: number,
    project?: string,
    variableName?: string,
    includeSensitiveVariables?: boolean
): Promise<FetchResponse<IPartialAutoCompleteResult>> => {
    return fetch({
        url: coreApi("/variableTemplate/completion"),
        method: "POST",
        body: {
            inputString,
            cursorPosition,
            project,
            variableName,
            includeSensitiveVariables,
        },
    });
};
