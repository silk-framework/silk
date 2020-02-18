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

export interface IFileWidgetItem {
    name: string;
    relativePath: string;
    absolutePath: string;
    size: number;
    modified: string;
}

export interface IWarningWidget {
    results: IWarningWidgetItem[];
}

export interface IFilesWidget {
    results: IFileWidgetItem[];
}

export interface IWidgetsState {
    /**
     * Store Project details page all widgets by widget name
     */
    configuration: IWorkspaceConfigurationWidget;

    warnings: IWarningWidget;

    files: IFilesWidget;
}
