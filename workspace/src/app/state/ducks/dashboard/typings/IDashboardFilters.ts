import { IPaginationState } from "../../../typings";

export type SortModifierType = 'ASC' | 'DESC' | '';

export interface IAppliedSorterState {
    sortBy: string;
    sortOrder: SortModifierType;
}

export interface ISorterListItemState {
    id: string;
    label: string;
}

export interface ISortersState {
    list: ISorterListItemState[];
    applied: IAppliedSorterState;
}

export interface IFacetValuesState {
    count: number;
    id: string;
    label: string;
}

export interface IFacetState {
    id: string;
    label: string;
    description: string;
    type: string;
    values: IFacetValuesState[]
}

export interface IAppliedFacetState {
    facetId: string;
    type: string;
    keywordIds: string[];
}

export interface IAppliedFiltersState {
    textQuery: string;
    sortBy?: string;
    sortOrder?: string;
    itemType?: string;
}

export interface IModifierOption {
    id: string;
    label: string;
}

export interface IModifierState {
    label: string;
    field: string;
    options: IModifierOption[];
}

export interface IModifiersState {
    [key: string]: IModifierState
}

export interface IFiltersState {
    facets: IFacetState[];
    modifiers: IModifiersState;
    appliedFilters: IAppliedFiltersState;
    appliedFacets: IAppliedFacetState[];
    pagination: IPaginationState;
    sorters: ISortersState;
}
