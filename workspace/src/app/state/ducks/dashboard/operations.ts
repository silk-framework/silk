import isEmpty from 'ramda/es/isEmpty';
import { batch } from "react-redux";

import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";
import { getApiEndpoint, getLegacyApiEndpoint } from "../../../utils/getApiEndpoint";

const {
    fetchTypeModifier,
    updateModifiers,
    updateResultTotal,
    updateFacets,
    updateSorters,
    applySorter,
    applyFilter,
    applyFacet,
    changePage,
    removeFacet
} = filtersSlice.actions;

const {
    setLoading,
    setError,
    cloneTask,
    fetchList,
    fetchListSuccess,
} = previewSlice.actions;

/**
 * Fetch types modifier
 */
const fetchTypesAsync = () => {
    return async dispatch => {
        batch(() => {
            dispatch(setLoading(true));
            dispatch(fetchTypeModifier());
        });
        try {
            const res = await fetch({
                url: getApiEndpoint('/searchConfig/types'),
                method: 'GET',
            });
            const validModifier = asModifier('Type', 'itemType', res.data);
            batch(() => {
                dispatch(setLoading(false));
                dispatch(updateModifiers({
                    fieldName: 'type',
                    modifier: validModifier
                }));
            });
        } catch (e) {
            dispatch(setError(e));
        }
    }
};

/**
 * Fetch the search results
 * by provided filters
 */
const fetchListAsync = () => {
    return async (dispatch, getState) => {
        batch(() => {
            dispatch(setLoading(true));
            dispatch(fetchList());
        });
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
                url: getApiEndpoint('/searchItems'),
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
                dispatch(setLoading(false));
            })
        } catch (err) {
            batch(() => {
                dispatch(setLoading(false));
                dispatch(setError(err));
            })
        }
    }
};

const getTaskMetadataAsync = async (taskId: string, projectId: string) => {
    const url = getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadata`);
    const { data } = await fetch({
        url
    });
    return data;
};

const fetchRemoveTaskAsync = (taskId: string, projectId: string) => {
    return async dispatch => {
        dispatch(setLoading(true));
        try {
            await fetch({
                url: getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}?removeDependentTasks=true`),
                method: 'DELETE',
            });
            batch(() => {
                dispatch(fetchListAsync());
                dispatch(setLoading(false));
            });
        } catch (e) {
            batch(() => {
                dispatch(setError(e));
                dispatch(setLoading(false));
            });
        }
    }
};

export default {
    fetchTypesAsync,
    fetchListAsync,
    fetchRemoveTaskAsync,
    getTaskMetadataAsync,
    cloneTask,
    applyFilter,
    applySorter,
    changePage,
    applyFacet,
    removeFacet
};
