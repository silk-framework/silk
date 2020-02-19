export interface IPrefixState {
    /**
     * Name of prefix
     */
    prefixName: string;
    /**
     * Name of prefix Uri
     */
    prefixUri: string;
}

export interface IWorkspaceConfigurationWidget {
    /**
     * Array of prefixes List
     */
    prefixes: IPrefixState[];
    /**
     * Plain object  for new prefix
     */
    newPrefix: IPrefixState;

    isLoading: boolean;

    error: any;
}

export interface IWarningWidgetItem {
    taskId: string;
    errorSummary: string;
    taskLabel: string;
    errorMessage: string;
    stackTrace: {
        errorMessage: string;
        lines: string[];
    }
}

export interface IWarningWidget {
    results: IWarningWidgetItem[];
    isLoading: boolean;
    error: any;
}

export interface IFileWidgetItem {
    name: string;
    size: number;
    lastModified: string;
}

export interface IFilesWidget {
    results: IFileWidgetItem[];
    isLoading: boolean;
    error: any;
}

export interface IWidgetsState {
    /**
     * Store Project details page all widgets by widget name
     */
    configuration: IWorkspaceConfigurationWidget;

    warnings: IWarningWidget;

    files: IFilesWidget;
}
