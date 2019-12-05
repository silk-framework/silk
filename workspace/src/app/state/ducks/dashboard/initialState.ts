import { IPaginationState } from "../../typings";
import {
    IAppliedFacetState,
    IAppliedFiltersState,
    IAppliedSorterState,
    IFiltersState,
    IModifierState,
    IPreviewState,
    ISortersState
} from "./typings";

export function initialPaginationState(props: Partial<IPaginationState> = {}): IPaginationState {
    return {
        limit: 5,
        offset: 0,
        current: 1,
        total: 0,
        ...props
    }
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

export function initialModifierState(props: Partial<IModifierState> = {}): IModifierState {
    return {
        label: '',
        field: '',
        options: [],
        ...props
    }
}

export function initialAppliedFacetState(props: Partial<IAppliedFacetState> = {}): IAppliedFacetState {
    return {
        facetId: '',
        type: '',
        keywordIds: [],
        ...props
    }
}

export function initialPreviewState(props: Partial<IPreviewState> = {}): IPreviewState {
    return {
        searchResults: [],
        editingTasks: {},
        isLoading: false,
        error: {},
        ...props
    }
}
