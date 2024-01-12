import {IFiltersState} from "./IWorkspaceFilters";
import {IPreviewState} from "./IWorkspacePreview";
import {IWidgetsState} from "@ducks/workspace/typings/IWorkspaceWidgets";
import {IItemLink} from "@ducks/shared/typings";

export * from "./IWorkspacePreview";
export * from "./IWorkspaceFilters";
export * from "./IWorkspaceWidgets";

export interface IWorkspaceState {
    filters: IFiltersState;
    preview: IPreviewState;
    widgets: IWidgetsState;
}

export interface ITaskLink {
    id: string;
    label: string;
    taskType: string;
}

/** Interface of a project import details request response. */
export interface IProjectImportDetails {
    // Project ID extracted from the uploaded project archive.
    projectId: string;
    // Project label from the archive
    label: string;
    // Project description from the archive
    description?: string;
    // True if a project with the same ID already exists
    projectAlreadyExists: boolean;
    // Error message stating that something went wrong and the project cannot be imported.
    errorMessage?: string;
}

/** The project execution status of a started project import. */
export interface IProjectExecutionStatus {
    // The project ID of the to be imported project.
    projectId: string;
    // Timestamp when the import has started
    importStarted: number;
    // Timestamp when the import has ended
    importEnded?: number;
    // If the import has been successful
    success?: boolean;
    // If something went wrong, these are the failure details.
    failureMessage?: string;
}

/** Recently viewed item. Either a project or task. */
export interface IRecentlyViewedItem {
    projectId: string;
    projectLabel: string;
    // E.g. "workflow", "transform", "task", "dataset"
    itemType: string;
    taskId?: string;
    taskLabel?: string;
    pluginId?: string;
    pluginLabel?: string;
    itemLinks: IItemLink[];
    tags?: Keywords;
}

export type Keyword = {
    label: string;
    uri: string;
};
export type Keywords = Array<Keyword>;

/** Additional information about a task context. */
export interface TaskContextResponse {
    inputTasks: TaskMetaData[],
    outputTasks: TaskMetaData[],
    originalInputs?: boolean,
    originalOutputs?: boolean
}

export interface TaskMetaData {
    taskId: string,
    label: string,
    isDataset: boolean,
    fixedSchema: boolean
}
