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
    project?: string
): Promise<FetchResponse<ValidateTemplateResponse>> => {
    return fetch({
        url: coreApi("/variableTemplate/validation"),
        method: "POST",
        body: {
            templateString,
            project,
        },
    });
};

/** Auto-complete a variable template. */
export const requestAutoCompleteTemplateString = async (
    inputString: string,
    cursorPosition: number,
    project?: string
): Promise<FetchResponse<IPartialAutoCompleteResult>> => {
    return fetch({
        url: coreApi("/variableTemplate/completion"),
        method: "POST",
        body: {
            inputString,
            cursorPosition,
            project,
        },
    });
};
