export interface IBreadcrumbState {
    href: string;
    text: string;
}

export interface IGlobalState {
    locale: string;
    authenticated: boolean;
    searchQuery: string;
    error?: any;
    breadcrumbs: IBreadcrumbState[];
    availableDataTypes: {}
}
