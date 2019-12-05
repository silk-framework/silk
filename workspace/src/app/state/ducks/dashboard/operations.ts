import isEmpty from 'ramda/es/isEmpty';
import { batch } from "react-redux";

import { API_ENDPOINT } from "../../../constants";

import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";


const {
    fetchTypeModifier,
    updateModifiers,
    updateResultTotal,
    updateFacets,
    updateSorters,
    applySorter,
    applyFilter,
    applyFacet,
    changePage
} = filtersSlice.actions;

const  {
    cloneTask,
    fetchList,
    fetchListFailure,
    fetchListSuccess,
} = previewSlice.actions;

/**
 * Fetch types modifier
 */
const fetchTypesAsync = () => {
    return async dispatch => {
        dispatch(fetchTypeModifier());
        const res = await fetch({
            url: API_ENDPOINT + '/searchConfig/types',
            method: 'GET',
        });
        const validModifier = asModifier('Type', 'itemType', res.data);
        dispatch(updateModifiers({
            fieldName: 'type',
            modifier: validModifier
        }));
    }
};

/**
 * Fetch the search results
 * by provided filters
 */
const fetchListAsync = () => {
    return async (dispatch, getState) => {
        dispatch(fetchList());
        // get applied pagination values
        const state = getState();
        const {limit, offset} = selectors.paginationSelector(state);
        const appliedFilters = selectors.appliedFiltersSelector(state);
        const sorters = selectors.sortersSelector(state);

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

        try {
            const res = await fetch({
                url: API_ENDPOINT + '/searchItems',
                method: 'post',
                body
            });

            const {total, facets, results, sortByProperties} = res.data;
            batch(() => {
                // Update the pagination total value
                dispatch(updateResultTotal(total));
                // Add the facets if it's presented
                dispatch(updateFacets(facets));
                // Add sorters
                dispatch(updateSorters(sortByProperties));
                // Apply results
                dispatch(fetchListSuccess(results));
            })
        } catch (err) {
            dispatch(fetchListFailure(err));
        }
    }
};

export default {
    fetchTypesAsync,
    fetchListAsync,
    cloneTask,
    applyFilter,
    applySorter,
    changePage,
    applyFacet,
};
