import { IFiltersState, initialFiltersState } from "./filters/dtos/Filter.dto";
import { initialPreviewState, IPreviewState } from "./preview/dtos";

export interface IDashboardState {
    filters: IFiltersState;
    preview: IPreviewState;
}

export function initialDashboardState(): IDashboardState {
    return {
        filters: initialFiltersState(),
        preview: initialPreviewState(),
    }
}

