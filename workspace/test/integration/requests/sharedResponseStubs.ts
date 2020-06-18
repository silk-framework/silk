/** A single task parameter and value. */

export interface ITaskParameter {
    id: string;
    value: string | Object;
    // optional label, if parameter value has a label, e.g. for a task parameter or enum parameter
    label?: string;
}

interface ITaskDataResponseParams {
    taskId?: string;
    taskLabel?: string;
    taskDescription?: string;
    parameters?: ITaskParameter[];
    withLabels?: boolean;
    project?: string;
    // e.g. "Dataset", "Task" etc.
    taskType?: string;
    // The concrete plugin ID, e.g. "csv", "xml", "transform" etc.
    pluginId?: string;
}

/** Returns a valid response for a task data request */
export const requestTaskDataTestResponse = ({
    parameters = [],
    withLabels = true,
    project = "unsetProjectId",
    taskType = "Task",
    pluginId = "unsetPluginType",
    taskId = "unsetTaskId",
    taskLabel = "unsetTaskLabel",
    taskDescription = "unsetTaskDescription",
}: ITaskDataResponseParams = {}) => {
    const paramJSon = {};
    parameters.forEach((param) => {
        let paramValue = param.value;
        if (withLabels) {
            paramValue = {
                value: param.value,
                label: param.label,
            };
        }
        paramJSon[param.id] = paramValue;
    });
    return {
        data: {
            parameters: paramJSon,
            taskType: taskType,
            type: pluginId,
        },
        id: taskId,
        metadata: {
            description: taskDescription,
            label: taskLabel,
            modified: "2020-05-05T00:00:00Z",
        },
        project: project,
        taskType: taskType,
    };
};
