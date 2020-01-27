import { IFiltersState } from "./IWorkspaceFilters";
import { IPreviewState } from "./IWorkspacePreview";

export interface IWorkspaceState {
    filters: IFiltersState;
    preview: IPreviewState;
}

export * from './IWorkspacePreview';
export * from './IWorkspaceFilters';
