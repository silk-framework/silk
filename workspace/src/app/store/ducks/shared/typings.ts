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
    name: string;
    description?: string;
    metaData: {
        create: string;
        description: string;
        modified: string;
        label: string;
    };
    tasks: any;
}

export interface ITaskMetadataResponse {
    taskType: string;
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
    previewContent: IPreviewContent;
}

/** The actual values of the preview */
export interface IPreviewContent {
    attributes: string[];
    values: string[][][];
}

export interface IMetadata {
    label: string;
    description: string;
    relations: IRelations;
    type?: string;
}
