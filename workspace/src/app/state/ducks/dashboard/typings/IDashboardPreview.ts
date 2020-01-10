export type DATA_TYPES = "project" | "Dataset" | "transform" | "Linking" | "task";

export interface ITaskItemLinks {
    label: string;
    path: string;
}

export interface ISearchResultsTask {
    description: string;
    id: string;
    label: string;
    type: string;
    projectId: string;
    itemLinks: ITaskItemLinks[];
}

export interface IPreviewState {
    searchResults: ISearchResultsTask[];
    editingTasks: {
        [key: string]: ISearchResultsTask
    };
    isLoading: boolean;
    error: any;
}

