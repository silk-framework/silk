export interface OriginalTaskData {
    pluginId: string;
    parameterValues: {
        parameters: TaskParameterValues;
        templates?: TaskParameterValues;
    };
}
export interface TaskParameterValues {
    [key: string]: TaskParameterValues | string;
}
