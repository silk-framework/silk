import { IPaginationState } from "../../../typings";

export type SortModifierType = 'ASC' | 'DESC' | '';

export interface IAppliedSorterState {
    /**
     * Sort field name
     */
    sortBy: string;
    /**
     * Direction of sort 'ASC' or 'DESC'
     */
    sortOrder: SortModifierType;
}

export interface ISorterListItemState {
    /**
     * Sorter id
     */
    id: string;
    /**
     * Sorter label
     */
    label: string;
}

export interface ISortersState {
    /**
     * Contains available sorters, received from search results request
     */
    list: ISorterListItemState[];
    /**
     * Applied sorters with directions
     */
    applied: IAppliedSorterState;
}

export interface IFacetKeywordsState {
    /**
     * Showing the available count of items for the current facet
     */
    count: number;
    /**
     * Keyword's id
     */
    id: string;
    /**
     * Keyword's label
     */
    label: string;
}

export interface IFacetState {
    /**
     * Facet's id
     */
    id: string;
    /**
     * Facets's label
     */
    label: string;
    /**
     * Facets's description
     */
    description: string;
    /**
     * Facet's type
     */
    type: string;
    /**
     * Array of Facet keywords
     */
    values: IFacetKeywordsState[]
}

export interface IAppliedFacetState {
    /**
     * Selected facet id
     */
    facetId: string;
    /**
     * Selected facet type
     */
    type: string;
    /**
     * Contains selected keyword ids facet
     */
    keywordIds: string[];
}

export interface IAppliedFiltersState {
    /**
     * search query for search results query
     */
    textQuery: string;
    /**
     * selected object type for search results query
     */
    itemType?: string;
}

export interface IFiltersState {
    /**
     * Array of available facets for current filter
     * received from search results request
     */
    facets: IFacetState[];
    /**
     * Applied filters for page, which contains
     * search query, selected types etc.
     */
    appliedFilters: IAppliedFiltersState;
    /**
     * Array of applied facets from facets list
     */
    appliedFacets: IAppliedFacetState[];
    /**
     * Pagination information received from search results query
     * also added additional `current` property, which store the current page number
     */
    pagination: IPaginationState;
    /**
     * Available and applied sorters for search result query
     */
    sorters: ISortersState;
}
