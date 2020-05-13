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
    properties?: string[];
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
    title?: string;
    description?: string;
    categories?: string[];
    markdownDocumentation?: string;
}

export interface IArtefactModal {
    isOpen: boolean;
    loading: boolean;
    artefactsList: IArtefactItem[];
    selectedArtefact: IArtefactItem;
    cachedArtefactProperties: {
        [key: string]: IDetailedArtefactItem;
    };
    selectedDType: string;
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
