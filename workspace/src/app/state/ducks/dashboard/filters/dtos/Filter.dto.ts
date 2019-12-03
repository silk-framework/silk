import { IAppliedFacetState, IFacetState } from "./Facets.dto";
import { IModifiersState } from "./Modifiers.dto";
import { initialPaginationState, IPaginationState } from "../../../../dto";

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
}

export function initialFiltersState(props: Partial<IFiltersState> = {}): IFiltersState {
    return {
        facets: [],
        modifiers: {},
        appliedFilters: initialAppliedFiltersState(),
        pagination: initialPaginationState(),
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
