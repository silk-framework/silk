import { FileBaseInfo } from "@ducks/shared/typings";

export interface IPrefixDefinition {
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
    prefixes: IPrefixDefinition[];
    /**
     * Plain object  for new prefix
     */
    newPrefix: IPrefixDefinition;

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
    };
}

export interface IWarningWidget {
    results: IWarningWidgetItem[];
    isLoading: boolean;
    error: any;
}

export interface IFileWidgetItem extends FileBaseInfo {
    size?: number;
    modified?: string;
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
