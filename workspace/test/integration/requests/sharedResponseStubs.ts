import { IProjectTask, TaskType } from "../../../src/app/store/ducks/shared/typings";
import { IArtefactItemProperty, IPluginDetails } from "../../../src/app/store/ducks/common/typings";

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
    taskType?: TaskType;
    // The concrete plugin ID, e.g. "csv", "xml", "transform" etc.
    pluginId?: string;
}

/** Returns a valid response for a task data request */
export const requestTaskDataTestResponse = ({
    parameters = [],
    withLabels = true,
    project = "unsetProjectId",
    taskType = "CustomTask",
    pluginId = "unsetPluginType",
    taskId = "unsetTaskId",
    taskLabel = "unsetTaskLabel",
    taskDescription = "unsetTaskDescription",
}: ITaskDataResponseParams = {}): IProjectTask => {
    const paramJSon = Object.create(null);
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

interface IArtefactPropertiesTestResponseParams {
    categories?: string[];
    description?: string;
    pluginId?: string;
    pluginLabel?: string;
    taskType?: TaskType;
    properties?: {
        [key: string]: IArtefactItemProperty;
    };
}

type ParameterType =
    | "resource"
    | "string"
    | "int"
    | "multiline string"
    | "char"
    | "traversable[string]"
    | "Long"
    | "double"
    | "boolean"
    | "option[int]"
    | "option[identifier]"
    | "stringmap"
    | "uri"
    | "project"
    | "task"
    | "identifier"
    | "restriction"
    | "enumeration"
    | "password"
    | "SPARQL endpoint";

// Immutable class to generate parameter descriptions
export class ParameterDescriptionGenerator {
    private readonly value: IArtefactItemProperty;

    constructor(value: Partial<IArtefactItemProperty> = {}) {
        this.value = {
            title: "unsetTitle",
            description: "unsetDescription",
            type: "string",
            value: "",
            advanced: false,
            parameterType: "string",
            visibleInDialog: true,
            ...value,
        };
    }

    withValues(updateValues: Partial<IArtefactItemProperty>): ParameterDescriptionGenerator {
        return new ParameterDescriptionGenerator({ ...this.value, ...updateValues });
    }

    setParameterType(parameterType: ParameterType): ParameterDescriptionGenerator {
        return new ParameterDescriptionGenerator({ ...this.value, parameterType: parameterType });
    }

    parameter(): IArtefactItemProperty {
        return { ...this.value };
    }
}

/** Returns response of the property schema of a plugin. */
export const requestArtefactPropertiesTestResponse = ({
    categories = ["unsetCategory"],
    description = "unset description",
    pluginId = "unsetPluginId",
    pluginLabel = "unsetPluginLabel",
    taskType = "CustomTask",
    properties = {},
}: IArtefactPropertiesTestResponseParams): IPluginDetails => {
    return {
        categories: categories,
        description: description,
        pluginId: pluginId,
        properties: properties,
        required: ["fileParameter"],
        taskType: taskType,
        title: pluginLabel,
        type: "object",
    };
};
