import { IAppliedFacetState, IFacetState } from "./Facets.dto";
import { IModifiersState } from "./Modifiers.dto";
import { initialPaginationState, IPaginationState } from "../../../../dto";

export interface IAppliedSorterState {
    sortBy: string;
    sortOrder: 'ASC' | 'DESC';
}

export interface ISorterListItemState {
    id: string;
    label: string;
}

export interface ISortersState {
    list: ISorterListItemState[];
    applied: IAppliedSorterState;
}

export interface IAppliedFiltersState {
    textQuery: string;
    sortBy?: string;
    sortOrder?: string;
    facets: IAppliedFacetState[];
}

export interface IFiltersState {
    facets: IFacetState[];
    modifiers: IModifiersState;
    appliedFilters: IAppliedFiltersState;
    pagination: IPaginationState;
    sorters: ISortersState;
}

export function initialFiltersState(props: Partial<IFiltersState> = {}): IFiltersState {
    return {
        facets: [],
        modifiers: {},
        appliedFilters: initialAppliedFiltersState(),
        pagination: initialPaginationState(),
        sorters: initialSortersState(),
        ...props
    }
}

export function initialSortersState(props: Partial<ISortersState> = {}): ISortersState {
    return {
        list: [],
        applied: {} as IAppliedSorterState,
        ...props
    }
}

export function initialAppliedFiltersState(props: Partial<IAppliedFiltersState> = {}): IAppliedFiltersState {
    return {
        textQuery: '',
        facets: [],
        ...props
    }
}
