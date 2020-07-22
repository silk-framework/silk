export interface IRequestAutocompletePayload {
    pluginId: string;
    parameterId: string;
    projectId: string;
    dependsOnParameterValues: string[];
    textQuery: string;
    limit: number;
    offset: number;
}

export interface IRelations {
    inputTasks: [];
    outputTasks: [];
    referencedTasks: [];
    dependentTasksDirect: [];
    dependentTasksAll: [];
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

/** The data of a project task from the generic /tasks endpoint. */
export interface IProjectTask {
    // Meta data of the project task
    metadata: IMetadata;
    // The task type that must be send to the backend, e.g. on POST PUT requests for task creation.
    taskType: TaskType;
    project: string;
    // item ID
    id: string;
    // The actual content
    data: {
        // The plugin ID
        type: string;
        // current parameter values
        parameters: {
            // If requested with withLabels option, then the values will be reified like this: {label: string, value: string | object}
            [key: string]: string | object;
        };
        taskType?: TaskType;
    };
}

export interface ITaskMetadataResponse {
    taskType: TaskType;
    schemata: any;
    type?: string;
    modified: string;
    project: string;
    label: string;
    id: string;
    relations: IRelations;
}

export interface IMetadataUpdatePayload {
    label: string;
    description?: string;
}

export interface IRelatedItem {
    id: string;
    label: string;
    type: string;
    itemLinks: IItemLink[];
}

export interface IItemLink {
    label: string;
    path: string;
    itemType: string;
}

export interface IItemInfo {
    itemType: {
        id: string;
        label: string;
    };
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

/**
 * A dataset preview request.
 */
export interface IDatasetConfigPreview {
    /** The project ID. Referenced resources must be located in this project. */
    project: string;
    datasetInfo: IDatasetInfo;
}

/**
 * The dataset configuration.
 */
export interface IDatasetInfo {
    /** The plugin ID, e.g. 'csv'. */
    type: string;
    /** The parameters of the plugin. */
    parameters: Record<string, string>;
}

/**
 * A file resource preview request. Usually this should not be used and a full dataset config should be given.
 */
export interface IResourcePreview {
    /** The project ID of the project the resource is located in. */
    project: string;
    /** The resource name of an existing project resource. */
    resource: string;
}

/** A preview request for an existing dataset */
export interface IDatasetPreview {
    project: string;
    /** The dataset ID */
    dataset: string;
    /** The type, in case of multi-type datasets like RDF, JSON etc. are used. If not specified it uses the "default" type of the dataset.  */
    typeUri?: string;
}

export interface IPreviewResponse {
    dataInfo: IDatasetInfo;
    previewType: string;
    previewContent?: IPreviewContent;
}

/** The actual values of the preview */
export interface IPreviewContent {
    attributes: string[];
    values: CellType[][];
}

// Depending on the preview type, either multiple values or one value of concatenated values is returned
type CellType = string | string[];

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

export interface IResourceListResponse {
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
