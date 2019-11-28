import fetch from '../../../providers/fetch';

import { API_ENDPOINT } from "../../../constants";
import { batch } from "react-redux";
import { dashboardSel } from "./index";
import {
    applyFilter,
    changePage,
    fetchTypeMod,
    fetchTypeModFailure,
    fetchTypeModSuccess,
    updateResultsTotal
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
            method:'GET',
        }).then(res => {
            const validModifier = asModifier('Type', 'itemType', res.data);
            dispatch(fetchTypeModSuccess(validModifier));
        }, err => {
            dispatch(fetchTypeModFailure(err));
        })
    }
};

/**
 *
 * @param type
 * @param filter
 */
const setFilterAsync = (type: string, filter: string) => {
    return dispatch => {
        batch(() => {
            dispatch(applyFilter(type, filter));
            dispatch(fetchListAsync());
        });
    }
};

const setSearchQueryAsync = (value: string) => {
    return dispatch => {
        dispatch(setFilterAsync('textQuery', value));
    }
};

/**
 * @param page
 */
const changePageAsync = (page: number) => {
    return dispatch => {
        batch(() => {
            dispatch(changePage(page));
            dispatch(fetchListAsync());
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
        const body = {
            limit,
            offset,
        };

        // get filters
        Object.keys(appliedFilters)
            .forEach(facet => {
                body[facet] = appliedFilters[facet]
            });

        fetch({
            url: API_ENDPOINT + '/searchItems',
            method:'post',
            body
        }).then(res => {
            batch(() => {
                dispatch(updateResultsTotal(res.data.total));
                dispatch(fetchListSuccess(res.data.results));
            })
        }, err => {
            dispatch(fetchListFailure(err));
        })
    }
};

export default {
    fetchTypesAsync,
    setFilterAsync,
    changePageAsync,
    setSearchQueryAsync,
    fetchListAsync
};
