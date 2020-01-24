import { batch } from "react-redux";

import fetch from '../../../services/fetch';

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";
import { getApiEndpoint, getLegacyApiEndpoint } from "../../../utils/getApiEndpoint";
import { routerOp } from "@ducks/router";
import { IFacetState } from "@ducks/dashboard/typings";
import { dashboardSel } from "@ducks/dashboard/index";
import qs from "query-string";

const {
    updateResultTotal,
    updateFacets,
    updateSorters,
    applySorter,
    applyFilters,
    applyFacet,
    changePage,
    removeFacet,
    resetFilters
} = filtersSlice.actions;

const {
    setLoading,
    setError,
    cloneTask,
    fetchList,
    fetchListSuccess,
    setProjectId,
    setProject,
    unsetProject
} = previewSlice.actions;

const ARRAY_DELIMITER = '|';

/**
 * Update the search query in url
 */
const updateQueryString = () => {
    return (dispatch, getState) => {
        const state = getState();

        const appliedFilters = dashboardSel.appliedFiltersSelector(state);
        const {applied: appliedSorters} = dashboardSel.sortersSelector(state);
        const appliedFacets = dashboardSel.appliedFacetsSelector(state);
        const {current} = dashboardSel.paginationSelector(state);

        const queryParams = {
            ...appliedFilters,
            ...appliedSorters,
            page: current,
            f_ids: appliedFacets.map(o => o.facetId),
            types: appliedFacets.map(o => o.type),
            f_keys: appliedFacets.map(o => o.keywordIds.join(ARRAY_DELIMITER))
        };
        dispatch(routerOp.setQueryString(queryParams));
    }
};

/*
* Setup Filters, Sorting, Pagination, Facets from Query string
**/
const setupFiltersFromQs = (queryString: string) => {
    return dispatch => {
        try {
            const parsedQs = qs.parse(queryString, {arrayFormat: "comma"});

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
                const facetIds = parsedQs.f_ids;
                if (!Array.isArray(facetIds)) {
                    batchQueue.push(applyFacet({
                        facet: {
                            id: facetIds,
                            type: parsedQs.types
                        },
                        keywordIds: [].concat(parsedQs.f_keys)
                    }));
                } else {
                    facetIds.forEach((facetId, i) => {
                        const facet: Partial<IFacetState> = {
                            id: facetId,
                            type: parsedQs.types[i]
                        };
                        batchQueue.push(applyFacet({
                            facet,
                            keywordIds: parsedQs.f_keys[i].split(ARRAY_DELIMITER)
                        }));
                    });
                }


            }

            // Pagination
            if (parsedQs.page) {
                batchQueue.push(
                    changePage(+parsedQs.page)
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

        } catch {
        }

    }
};

const fetchProjectMetadata = () => {
    return async (dispatch, getState) => {
        batch(() => {
            dispatch(setError({}));
            dispatch(setLoading(true));
        });

        try {
            const projectId = selectors.currentProjectIdSelector(getState());
            const res = await fetch({
                url: getLegacyApiEndpoint(`/projects/${projectId}`),
            });

            batch(() => {
                // Apply results
                dispatch(setProject(res.data));
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
        const appliedFacets = selectors.appliedFacetsSelector(state);
        const sorters = selectors.sortersSelector(state);
        const projectId = selectors.currentProjectIdSelector(state);

        const body: any = {
            limit,
            offset,
        };

        if (sorters.applied.sortBy) {
            body.sortBy = sorters.applied.sortBy;
            body.sortOrder = sorters.applied.sortOrder;
        }

        if (projectId) {
            body.project = projectId;
        }

        // get filters
        Object.keys(appliedFilters)
            .forEach(filter => {
                if (filter.length) {
                    body[filter] = appliedFilters[filter]
                }
            });

        // get facets
        body.facets = appliedFacets.map(facet => facet);

        try {
            const res = await fetch({
                url: getApiEndpoint('/searchItems'),
                method: 'post',
                body
            });

            const {total, facets, results, sortByProperties} = res.data;
            batch(() => {
                // Apply results
                dispatch(fetchListSuccess(results));
                dispatch(setLoading(false));
                // Update the pagination total value
                dispatch(updateResultTotal(total));
                // Add the facets if it's presented
                dispatch(updateFacets(facets));
                // Add sorters
                dispatch(updateSorters(sortByProperties));
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
            dispatch(fetchListAsync());
            dispatch(updateQueryString());
        });
    }
};

const applySorterOp = (sortBy: string) => {
    return dispatch => {
        batch(() => {
            dispatch(applySorter({
                sortBy
            }));
            dispatch(fetchListAsync());
            dispatch(updateQueryString());
        });
    }
};

const changePageOp = (page: number) => {
    return dispatch => {
        batch(() => {
            dispatch(changePage(page));
            dispatch(fetchListAsync());
            dispatch(updateQueryString());
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

        batch(() => {
            dispatch(fetchListAsync());
            dispatch(updateQueryString());
        })
    }
};

export default {
    fetchListAsync,
    fetchRemoveTaskAsync,
    getTaskMetadataAsync,
    fetchCloneTaskAsync,
    applyFiltersOp,
    applySorterOp,
    cloneTask,
    changePageOp,
    toggleFacetOp,
    setupFiltersFromQs,
    fetchProjectMetadata,
    resetFilters,
    setProjectId,
    unsetProject
};
