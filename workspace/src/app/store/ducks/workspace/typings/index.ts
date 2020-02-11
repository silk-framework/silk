import { IFiltersState } from "./IWorkspaceFilters";
import { IPreviewState } from "./IWorkspacePreview";
import { IWidgetsState } from "@ducks/workspace/typings/IWorkspaceWidgets";

export * from './IWorkspacePreview';
export * from './IWorkspaceFilters';
export * from './IWorkspaceWidgets';

export interface IWorkspaceState {
    filters: IFiltersState;
    preview: IPreviewState;
    widgets: IWidgetsState;
}
