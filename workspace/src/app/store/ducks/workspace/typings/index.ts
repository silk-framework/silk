import { IFiltersState } from "./IDashboardFilters";
import { IPreviewState } from "./IDashboardPreview";

export interface IDashboardState {
    filters: IFiltersState;
    preview: IPreviewState;
}

export * from './IDashboardPreview';
export * from './IDashboardFilters';
