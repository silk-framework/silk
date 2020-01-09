import { batch } from "react-redux";

import fetch from '../../../services/fetch';
import asModifier from "../../../utils/asModifier";

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";
import { getApiEndpoint, getLegacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { routerOp } from "@ducks/router";
import { IFacetState } from "@ducks/dashboard/typings";
import { dashboardSel } from "@ducks/dashboard/index";
import qs from "query-string";

const {
    fetchTypeModifier,
    updateModifiers,
    updateResultTotal,
    updateFacets,
    updateSorters,
    applySorter,
    applyFilters,
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
            dispatch(setError({}));
            dispatch(setLoading(true));
            dispatch(fetchTypeModifier());
        });
        try {
            const {data} = await fetch({
                url: getApiEndpoint('/searchConfig/types'),
                method: 'GET',
            });
            const validModifier = asModifier(data.label, 'itemType', data.values);
            batch(() => {
                dispatch(setLoading(false));
                dispatch(updateModifiers({
                    fieldName: 'type',
                    modifier: validModifier
                }));
            });
        } catch (e) {
            dispatch(setError(e.response.data));
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
            dispatch(setError({}));
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

        if (sorters.applied.sortBy) {
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
        } catch (e) {
            batch(() => {
                dispatch(setLoading(false));
                dispatch(setError(e.response.data));
            })
        }
    }
};

const getTaskMetadataAsync = async (taskId: string, projectId: string) => {
    const url = getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/metadata`);
    const {data} = await fetch({
        url
    });
    return data;
};

const fetchRemoveTaskAsync = (taskId: string, projectId: string) => {
    return async dispatch => {
        batch(() => {
            dispatch(setLoading(true));
            dispatch(setError({}));
        });

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
                dispatch(setError(e.response.data));
                dispatch(setLoading(false));
            });
        }
    }
};

const fetchCloneTaskAsync = (taskId: string, projectId: string, taskNewId: string) => {
    return async dispatch => {
        batch(() => {
            dispatch(setError({}));
            dispatch(setLoading(true));
        });

        try {
            await fetch({
                url: getLegacyApiEndpoint(`/projects/${projectId}/tasks/${taskId}/clone?newTask=${taskNewId}`),
                method: 'POST',
            });
            batch(() => {
                dispatch(fetchListAsync());
                dispatch(setLoading(false));
            });
        } catch (e) {
            batch(() => {
                dispatch(setError(e.response.data));
                dispatch(setLoading(false));
            });
        }
    }
};

const applyFiltersOp = (filter) => {
    return dispatch => {
        batch(() => {
            dispatch(applyFilters(filter));
            dispatch(routerOp.setQueryString(filter));
        });
    }
};

const applySorterOp = (sortBy: string) => {
    return (dispatch, getState) => {
        dispatch(applySorter({
            sortBy
        }));
        const sorters = dashboardSel.sortersSelector(getState());
        dispatch(routerOp.setQueryString(sorters.applied));
    }
};

const changePageOp = (page: number) => {
    return dispatch => {
        batch(() => {
            dispatch(changePage(page));
            dispatch(routerOp.setQueryString({
                page
            }));
        });
    }
};

const toggleFacetOp = (facet: IFacetState, keywordId: string) => {
    return (dispatch, getState) => {
        const facets = dashboardSel.appliedFacetsSelector(getState());
        const foundFacet = facets.find(o => o.facetId === facet.id);

        const isKeywordMissing = foundFacet && !foundFacet.keywordIds.includes(keywordId);

        if (!foundFacet || isKeywordMissing) {
            dispatch(applyFacet({
                facet,
                keywordIds: [keywordId]
            }));
        } else {
            dispatch(removeFacet({
                facet: foundFacet,
                keywordId
            }));
        }

        const updatedFacets = dashboardSel.appliedFacetsSelector(getState());
        const queryParams = {
            f_ids:  updatedFacets.map(o => o.facetId),
            types:  updatedFacets.map(o => o.type),
            f_keys: updatedFacets.map(o => o.keywordIds.join(','))
        };

        dispatch(routerOp.setQueryString(queryParams))
    }
};

/*
* Setup Filters, Sorting, Pagination, Facets from Query string
**/
const setupFiltersFromQs = (queryString: string) => {
    return dispatch => {
        try {
            const parsedQs = qs.parse(queryString, {
                parseNumbers: true,
                arrayFormat: "comma"
            });

            // The batch of functions that should dispatched
            const batchQueue = [];

            // setup filters
            const filters: any = {};
            if (parsedQs.textQuery) {
                filters.textQuery = parsedQs.textQuery;
            }
            if (parsedQs.itemType) {
                filters.itemType = parsedQs.itemType;
            }
            batchQueue.push(
                applyFilters(filters)
            );

            // Facets
            if (parsedQs.f_ids) {
                // @TODO: add array setup feature
                const facet: Partial<IFacetState> = {
                    id: parsedQs.f_ids as string,
                    type: parsedQs.types as string,
                };

                batchQueue.push(applyFacet({
                    facet,
                    keywordIds: parsedQs.f_keys
                }));
            }

            // Pagination
            if (parsedQs.page) {
                batchQueue.push(
                    changePage(parsedQs.page)
                )
            }

            // Sorting
            if (parsedQs.sortBy) {
                batchQueue.push(applySorter({
                    sortBy: parsedQs.sortBy,
                    sortOrder: parsedQs.sortOrder
                }))
            }

            batch(() => batchQueue.forEach(dispatch));

        } catch {}

    }
};

export default {
    fetchTypesAsync,
    fetchListAsync,
    fetchRemoveTaskAsync,
    getTaskMetadataAsync,
    fetchCloneTaskAsync,
    applyFiltersOp,
    applySorterOp,
    cloneTask,
    changePageOp,
    toggleFacetOp,
    setupFiltersFromQs
};
