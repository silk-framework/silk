
export interface IBreadcrumbState {
    href: string;
    text: string;
}

export interface IAvailableDataTypes {
    [key: string]: IAvailableDataType
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

export interface IArtefactItemProperty {
    [key: string]: {
        title: string;
        description: string;
        type: string;
        value: string;
        advanced: boolean;
    }
}

export interface IArtefactItem {
    key: string;
    title: string;
    description: string;
    type: string;
    categories: string[];
    properties: IArtefactItemProperty;
    required: string[];
}

export interface IArtefactModal {
    isOpen: boolean;
    artefactsList: IArtefactItem[];
    selectedArtefact: IArtefactItem;
}

export interface IGlobalState {
    locale: string;
    authenticated: boolean;
    searchQuery: string;
    error?: any;
    breadcrumbs: IBreadcrumbState[];
    availableDataTypes: IAvailableDataTypes;
    artefactModal: IArtefactModal;
}
