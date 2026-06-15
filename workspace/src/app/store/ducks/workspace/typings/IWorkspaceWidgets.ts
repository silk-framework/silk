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

export interface IDetailedProjectPrefixes {
    projectPrefixes: Record<string, string>;
    workspacePrefixes: Record<string, string>;
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
    warnings: IWarningWidget;

    files: IFilesWidget;
}
