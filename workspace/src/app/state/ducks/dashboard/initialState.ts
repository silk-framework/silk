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
        appliedFacets: [],
        pagination: initialPaginationState(),
        sorters: initialSortersState(),
        ...props
    }
}

export function initialAppliedSortersState(): IAppliedSorterState {
    return {
        sortBy: '',
        sortOrder: ''
    }
}

export function initialSortersState(props: Partial<ISortersState> = {}): ISortersState {
    return {
        list: [],
        applied: initialAppliedSortersState(),
        ...props
    }
}

export function initialAppliedFiltersState(props: Partial<IAppliedFiltersState> = {}): IAppliedFiltersState {
    return {
        textQuery: '',
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
        projectMetadata: {},
        currentProjectId: null,
        error: {},
        ...props
    }
}

