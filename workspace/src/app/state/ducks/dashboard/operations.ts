import isEmpty from 'ramda/es/isEmpty';

import fetch from '../../../providers/fetch';
import { API_ENDPOINT } from "../../../constants";
import { batch } from "react-redux";
import { dashboardSel } from "./index";
import {
    applyFacet,
    applyFilter,
    applySorter,
    changePage,
    fetchTypeMod,
    updateFacets,
    updateModifiers,
    updateResultsTotal, updateSorters
} from "./filters/actions";
import { fetchList, fetchListFailure, fetchListSuccess } from "./preview/actions";
import asModifier from "../../../utils/asModifier";

/**
 * Fetch types modifier
 */
const fetchTypesAsync = () => {
    return dispatch => {
        dispatch(fetchTypeMod());
        fetch({
            url: API_ENDPOINT + '/searchConfig/types',
            method: 'GET',
        }).then(res => {
            const validModifier = asModifier('Type', 'itemType', res.data);
            dispatch(updateModifiers('type', validModifier));
        }, error => {

        })
    }
};

/**
 * Fetch the search results
 * by provided filters
 */
const fetchListAsync = () => {
    return (dispatch, getState) => {
        dispatch(fetchList());
        // get applied pagination values
        const state = getState();
        const {limit, offset} = dashboardSel.paginationSelector(state);
        const appliedFilters = dashboardSel.appliedFiltersSelector(state);
        const sorters = dashboardSel.sortersSelector(state);

        const body: any = {
            limit,
            offset,
        };

        if (!isEmpty(sorters)) {
            body.sortBy = sorters.applied.sortBy;
            body.sortOrder = sorters.applied.sortOrder;
        }

        // get filters
        Object.keys(appliedFilters)
            .forEach(filter => {
                if (filter.length) {
                    body[filter] = appliedFilters[filter]
                }
            });

        fetch({
            url: API_ENDPOINT + '/searchItems',
            method: 'post',
            body
        }).then(res => {
            const {total, facets, results, sortByProperties} = res.data;
            batch(() => {
                // Update the pagination total value
                dispatch(updateResultsTotal(total));
                // Add the facets if it's presented
                dispatch(updateFacets(facets));
                // Add sorters
                dispatch(updateSorters(sortByProperties));
                // Apply results
                dispatch(fetchListSuccess(results));
            })
        }, err => {
            dispatch(fetchListFailure(err));
        })
    }
};

export default {
    fetchTypesAsync,
    fetchListAsync,
    applyFilter,
    applySorter,
    changePage,
    applyFacet
};
