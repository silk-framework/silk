import { IMetadata } from "@ducks/shared/typings";

export interface IAvailableDataTypes {
    [key: string]: IAvailableDataType;
}

export interface IAvailableDataTypeOption {
    id: string;
    label: string;
}

export interface IAvailableDataType {
    label: string;
    field: string;
    options: IAvailableDataTypeOption[];
}

/** Properties for parameter auto-completion. */
export interface IPropertyAutocomplete {
    allowOnlyAutoCompletedValues: boolean;
    autoCompleteValueWithLabels: boolean;
    autoCompletionDependsOnParameters: string[];
}

/** Description of a parameter of an item. */
export interface IArtefactItemProperty {
    title: string;
    description: string;
    // Either "string" or "object"
    type: string;
    value: string;
    advanced: boolean;
    parameterType: string;
    visibleInDialog: boolean;
    autoCompletion?: IPropertyAutocomplete;
    // in case of type=="object" this will be defined
    pluginId?: string;
    // in case of type=="object" this will be defined
    properties?: Record<string, IArtefactItemProperty>;
    // Lists the required parameters in case properties is defined
    required?: string[];
}

/** Parameter of a task. */
export interface ITaskParameter {
    paramId: string;
    param: IArtefactItemProperty;
}

/** The full task plugin description, including detailed schema. */
export interface IDetailedArtefactItem {
    title: string;
    description: string;
    taskType: string;
    type: string;
    categories: string[];
    properties: {
        [key: string]: IArtefactItemProperty;
    };
    required: string[];
    pluginId: string;
    markdownDocumentation?: string;
}

/** Overview version of an item description. */
export interface IArtefactItem {
    key: string;
    taskType: string;
    title?: string;
    description?: string;
    categories?: string[];
    markdownDocumentation?: string;
}

/** Contains all data that is needed to render an update dialog. */
export interface IProjectTaskUpdatePayload {
    projectId: string;
    taskId: string;
    taskPluginDetails: IDetailedArtefactItem;
    metaData: IMetadata;
    currentParameterValues: {
        [key: string]: string | object;
    };
}

export interface IArtefactModal {
    // If true, this modal is shown to the user
    isOpen: boolean;

    loading: boolean;

    // The list of item types that can be selected.
    artefactsList: IArtefactItem[];

    categories: {
        label: string;
        count: number;
    }[];

    // The selected item type
    selectedArtefact: IArtefactItem;

    // cached plugin descriptions
    cachedArtefactProperties: {
        [key: string]: IDetailedArtefactItem;
    };

    // The selected item category
    selectedDType: string;

    // If an existing task should be updated
    updateExistingTask?: IProjectTaskUpdatePayload;

    error: any;

    // If true, shows a spinner instead of the action buttons, so the user cannot click again while waiting
    actionLoading: boolean;
}

export interface ICommonState {
    /**
     * Used in Project details page only and store the current selected project id
     * Received from router
     */
    currentProjectId: string;
    currentTaskId: string;
    locale: string;
    initialSettings: any;
    authenticated: boolean;
    searchQuery: string;
    error?: any;
    availableDataTypes: IAvailableDataTypes;
    artefactModal: IArtefactModal;
}
