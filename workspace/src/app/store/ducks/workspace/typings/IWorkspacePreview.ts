export type DATA_TYPES = "project" | "Dataset" | "transform" | "Linking" | "task";

export interface ITaskItemLinks {
    label: string;
    path: string;
}

export interface ISearchResultsServer {
    /**
     * Description of item, project, task
     */
    description: string;
    /**
     * id of item, project, task
     */
    id: string;
    /**
     * Presentable name of item, project, task
     */
    label: string;
    /**
     * Type of search results
     */
    type: string;
    /**
     * Showing project id if it's presented
     */
    projectId: string;
    /**
     * Contains of useful links for items
     */
    itemLinks: ITaskItemLinks[];
}

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

