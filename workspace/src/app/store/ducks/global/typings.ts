
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

export interface IArtefactModal {
    isOpen: boolean;
    artefactsList: any[];
    selectedArtefact: string;
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
