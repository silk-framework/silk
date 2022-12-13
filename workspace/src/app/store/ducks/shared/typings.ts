import { Keywords } from "@ducks/workspace/typings";

export interface IRequestAutocompletePayload {
    pluginId: string;
    parameterId: string;
    projectId: string;
    dependsOnParameterValues: string[];
    textQuery: string;
    limit: number;
    offset: number;
}

export interface ITaskLink {
    id: string;
    label?: string;
    taskLink: string;
}

export interface IRelations {
    inputTasks: string[];
    outputTasks: string[];
    referencedTasks: string[];
    dependentTasksDirect: string[] | ITaskLink[];
    dependentTasksAll: string[] | ITaskLink[];
}

export interface IProjectMetadataResponse {
    label: string;
    description?: string;
    metaData: {
        create: string;
        description: string;
        modified: string;
        label: string;
    };
    tasks: any;
}
interface ITaskTypes {
    [key: string]: TaskType;
}
export const TaskTypes: ITaskTypes = {
    DATASET: "Dataset",
    LINKING: "Linking",
    TRANSFORM: "Transform",
    WORKFLOW: "Workflow",
    CUSTOM_TASK: "CustomTask",
};

export type TaskType = "Dataset" | "Linking" | "Transform" | "Workflow" | "CustomTask";

export type RuleOperatorType = "AggregationOperator" | "TransformOperator" | "ComparisonOperator";

export type PluginType = TaskType | RuleOperatorType;

export interface IArbitraryPluginParameters {
    // If requested with withLabels option, then the values will be reified like this: {label: string, value: string | object}
    [key: string]: string | object;
}

/** The data of a project task from the generic /tasks endpoint. */
export interface IProjectTask<PLUGIN_PARAMETERS = IArbitraryPluginParameters> {
    // Meta data of the project task
    metadata: IMetadata;
    // The task type that must be send to the backend, e.g. on POST PUT requests for task creation.
    taskType: TaskType;
    project: string;
    // item ID
    id: string;
    // The actual content
    data: TaskPlugin<PLUGIN_PARAMETERS>;
}

/** Task plugin. */
export interface TaskPlugin<PLUGIN_PARAMETERS = IArbitraryPluginParameters> {
    // The plugin ID
    type: string;
    // current parameter values
    parameters: PLUGIN_PARAMETERS;
    // Optional task type, e.g. Dataset, Transform etc.
    taskType?: TaskType;
    // Dataset may have a URI property set
    uriProperty?: string;
}

export interface DatasetTaskPlugin<PLUGIN_PARAMETERS = IArbitraryPluginParameters>
    extends TaskPlugin<PLUGIN_PARAMETERS> {
    /** The attribute/property (URI) that the entity URI should be written to. */
    uriProperty?: string;
}

export interface ITaskMetadataResponse {
    taskType: TaskType;
    schemata: any;
    type?: string;
    modified: string;
    project: string;
    label: string;
    id: string;
    description?: string;
    relations: IRelations;
}

/** A project or task item used in various modals. */
export interface IModalItem {
    projectId: string;
    projectLabel?: string;
    // If the id is set the item is a task, else a project.
    id?: string;
    type: string;
    label?: string;
    description?: string;
}

export interface IMetadataUpdatePayload {
    label: string;
    description?: string;
    tags?: string[];
}

export interface IRelatedItem {
    id: string;
    label: string;
    type: string;
    itemLinks: IItemLink[];
    pluginLabel: string;
    tags: Keywords;
    projectId?: string;
    pluginId?: string;
}

export interface IItemLink {
    id: string;
    label: string;
    path: string;
    itemType?: string;
}

export interface IRelatedItemsResponse {
    total: number;
    items: IRelatedItem[];
}

export interface IDatasetTypePayload {
    limit?: number;
    textQuery?: string;
}

export interface IDatasetTypesRequest {
    projectId: string;
    datasetId: string;
    textQuery?: string;
    limit?: number;
}

export interface IMetadata {
    label: string;
    description?: string;
    relations?: IRelations;
    type?: string;
    modified?: string;
}

export interface IResourceListPayload {
    /**
     * description: If defined the resources will be filtered by the search text which searches over the resource names.
     **/
    searchText?: string;
    /**
     * Limits the number of resources returned by this endpoint.
     */
    limit?: number;
    /**
     * The offset in the result list. Offset and limit allow paging over the results.
     */
    offset?: number;
}

/** Project file resource. */
export interface IProjectResource {
    /**
     * Last modification Datetime
     */
    modified: string;
    /**
     * The name of resource/file
     */
    name: string;
    /**
     * Resource/file size on bytes
     */
    size: number;
}

export interface IAutocompleteDefaultResponse {
    /**
     * The option name
     */
    label?: string;
    /**
     * The option value
     */
    value: string;
}
