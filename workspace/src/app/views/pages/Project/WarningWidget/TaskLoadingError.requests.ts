import { FetchResponse } from "../../../../services/fetch/responseInterceptor";
import { projectApi } from "../../../../utils/getApiEndpoint";
import fetch from "../../../../services/fetch";
import { OriginalTaskData, TaskParameterValues } from "./TaskLoadingError.typing";

/** Fetch the parameters of a failed task. */
export const failedTaskParameters = async (
    projectId: string,
    taskId: string
): Promise<FetchResponse<OriginalTaskData>> => {
    return fetch({
        url: projectApi(`/${projectId}/failedTaskParameters/${taskId}`),
    });
};

/** Fetch the parameters of a failed task. */
export const fixFailedTask = async (
    projectId: string,
    taskId: string,
    parameterData: TaskParameterValues,
    variableTemplateData: TaskParameterValues,
    dataParameters?: Record<string, string>
): Promise<FetchResponse<OriginalTaskData>> => {
    const payload = {
        taskId: taskId,
        parameterValues: {
            parameters: {
                ...parameterData,
                ...dataParameters,
            },
            templates: {
                ...variableTemplateData,
            },
        },
    };
    return fetch({
        url: projectApi(`/${projectId}/reloadFailedTask`),
        method: "POST",
        body: payload,
    });
};
