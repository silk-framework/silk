import * as types from "./types";
import { FiltersDto, Modifier } from "./dtos";
import { PaginationDto } from "../../../dto";

const dashboardFiltersReducers = (state = new FiltersDto(), action: any = {}): FiltersDto => {
    switch (action.type) {
        case (types.FETCH_TYPES_MOD):
            return {
                ...state,
                modifiers: {
                    ...state.modifiers,
                    type: new Modifier()
                },
            };

        case (types.FETCH_TYPES_MOD_SUCCESS):
            return {
                ...state,
                modifiers: {
                    ...state.modifiers,
                    type: action.payload.data
                }
            };

        case (types.FETCH_TYPES_MOD_FAILURE):
            return {
                ...state,
                // error: action.payload.error
            };

        case (types.FETCH_FACETS):
            return {
                ...state,
                facets: [],
            };

        case (types.FETCH_FACETS_SUCCESS):
            return {
                ...state,
                facets: [
                    ...state.facets,
                    action.payload.data
                ]
            };

        case (types.FETCH_FACETS_FAILURE):
            return {
                ...state,
                // error: action.payload.error
            };

        case (types.APPLY_FILTER):
            const updatedFilters = {...state.appliedFilters};
            const { type, filterValue } = action.payload;

            if (filterValue === '') {
                delete updatedFilters[type];
            } else {
                updatedFilters[type] = filterValue;
            }

            return {
                ...state,
                appliedFilters: {
                    ...updatedFilters,
                },
                pagination: new PaginationDto()
            };

        case (types.CHANGE_PAGE):
            const { page } = action.payload;
            const { pagination } = state;
            const offset = (page - 1) * pagination.limit + 1;

            return {
                ...state,
                pagination: {
                    ...pagination,
                    offset,
                    current: page
                }
            };

        case (types.UPDATE_RESULTS_TOTAL):
            return {
                ...state,
                pagination: {
                    ...state.pagination,
                    total: action.payload.total
                }
            };

        default:
            return state;
    }
};

export default dashboardFiltersReducers;
