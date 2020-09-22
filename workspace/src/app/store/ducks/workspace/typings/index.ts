import { IFiltersState } from "./IWorkspaceFilters";
import { IPreviewState } from "./IWorkspacePreview";
import { IWidgetsState } from "@ducks/workspace/typings/IWorkspaceWidgets";

export * from "./IWorkspacePreview";
export * from "./IWorkspaceFilters";
export * from "./IWorkspaceWidgets";

export interface IWorkspaceState {
    filters: IFiltersState;
    preview: IPreviewState;
    widgets: IWidgetsState;
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
