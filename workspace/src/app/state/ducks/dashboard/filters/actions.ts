import { createAction } from "@reduxjs/toolkit";
import { IFacetState, ISorterListItemState } from "./dtos";

const FETCH_TYPES_MOD = "dashboard/filters/FETCH_TYPES_MOD";

const UPDATE_MODIFIERS = "dashboard/filters/UPDATE_MODIFIERS";
const CHANGE_PAGE = "dashboard/filters/CHANGE_PAGE";
const UPDATE_RESULTS_TOTAL = "dashboard/filters/UPDATE_RESULTS_TOTAL";
const UPDATE_FACETS = "dashboard/filters/UPDATE_FACETS";
const APPLY_FACET = "dashboard/filters/APPLY_FACET";
const APPLY_FILTER = "dashboard/filters/APPLY_FILTER";
const UPDATE_SORTERS = "dashboard/filters/UPDATE_SORTERS";
const APPLY_SORTER = "dashboard/filters/APPLY_SORTER";

export const fetchTypeMod = createAction(FETCH_TYPES_MOD);

export const updateModifiers = createAction(
    UPDATE_MODIFIERS,
    (fieldName: string, modifier:  any) => ({
        payload: {
            fieldName,
            modifier
        }
    })
);

export const applyFilter = createAction(
    APPLY_FILTER,
    (field: string, value: string | number) => ({
        payload: {
            field,
            value
        }
    }));

export const updateSorters = createAction(
    UPDATE_SORTERS,
    (sorters: ISorterListItemState[]) => ({
        payload: {
            sorters
        }
    }));

export const applySorter = createAction(
    APPLY_SORTER,
    (sortBy: string) => ({
        payload: {
            sortBy
        }
    }));

export const changePage = createAction(
    CHANGE_PAGE,
    (page: number) => ({
        payload: {
            page
        }
    }));

export const updateResultsTotal = createAction(
    UPDATE_RESULTS_TOTAL,
    (total: number) => ({
        payload: {
            total
        }
    }));

export const updateFacets = createAction(
    UPDATE_FACETS,
    (facets: IFacetState) => ({
        payload: {
            facets
        }
    }));

export const applyFacet = createAction(
    APPLY_FACET,
    (facet: IFacetState, value: string) => ({
        payload: {
            facet,
            value
        }
    }));
