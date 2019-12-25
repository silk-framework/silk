import { DATA_TYPES } from "../../../../constants";

export type DATA_TYPES = keyof typeof DATA_TYPES;

export interface ITaskItemLinks {
    label: string;
    path: string;
}

export interface ISearchResultsTask {
    description: string;
    id: string;
    label: string;
    type: DATA_TYPES;
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

