import { Keywords } from ".";
import { TaskType } from "@ducks/shared/typings";

export type DATA_TYPES = "project" | "Dataset" | "transform" | "Linking" | "task";

export interface ITaskItemLinks {
    label: string;
    path: string;
}

export interface ISearchResultsServer {
    /**
     * Description of item, project, task
     */
    description?: string;
    /**
     * id of item, project, task
     */
    id: string;
    /**
     * Presentable name of item, project, task
     */
    label: string;
    /**
     * Type of search results, e.g. "dataset", "transform" etc.
     */
    type: string;
    /**
     * Showing project id if it's presented
     */
    projectId?: string;
    projectLabel?: string;
    /**
     * Contains of useful links for items
     */
    itemLinks?: ITaskItemLinks[];
    /**
     * Contains plugin name
     */
    pluginId?: string;

    /**
     * The label of the plugin.
     */
    pluginLabel?: string;

    /**
     * set tags for a project or task
     */
    tags?: Keywords;
}

export const convertTaskTypeToSearchResultServerType = (taskType: TaskType): string => {
    switch (taskType) {
        case "CustomTask":
            return "task";
        case "Dataset":
            return "dataset";
        case "Transform":
            return "transform";
        case "Workflow":
            return "workflow";
        case "Linking":
            return "linking";
        default:
            return "task";
    }
};

export interface IPreviewState {
    /**
     * Array of results, contains Projects and other types
     */
    searchResults: ISearchResultsServer[];
    /**
     * Loading indicator for Workspace and Project Details page
     */
    isLoading: boolean;
    /**
     * Error object which contains all kind of errors, presented in Workspace UI
     */
    error: any;
}
