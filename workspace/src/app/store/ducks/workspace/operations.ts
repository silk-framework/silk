import { batch } from "react-redux";

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";
import { routerOp } from "@ducks/router";
import { IFacetState } from "@ducks/workspace/typings";
import { workspaceSel } from "@ducks/workspace";
import qs from "qs";
import {
    fetchAddOrUpdatePrefixAsync,
    fetchProjectPrefixesAsync,
    fetchRemoveProjectPrefixAsync,
} from "@ducks/workspace/widgets/configuration.thunk";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { fetchWarningListAsync, fetchWarningMarkdownAsync } from "@ducks/workspace/widgets/warning.thunk";
import { checkIfResourceExistsAsync, fetchResourcesListAsync } from "@ducks/workspace/widgets/file.thunk";
import { commonSel } from "@ducks/common";
import {
    ISearchListRequest,
    requestCloneTask,
    requestCreateProject,
    requestCreateTask,
    requestRemoveProject,
    requestRemoveTask,
    requestSearchList,
} from "@ducks/workspace/requests";

const {
    updateResultTotal,
    updateFacets,
    updateSorters,
    applySorter,
    applyFilters,
    applyFacet,
    changePage,
    changeProjectsLimit,
    removeFacet,
    resetFilters,
} = filtersSlice.actions;

const { setLoading, setError, fetchList, fetchListSuccess } = previewSlice.actions;

const { updateNewPrefix } = widgetsSlice.actions;

const ARRAY_DELIMITER = "|";
const VALUE_DELIMITER = ",";

/**
 * Update the search query in url
 */
const updateQueryString = () => {
    return (dispatch, getState) => {
        const state = getState();

        const appliedFilters = workspaceSel.appliedFiltersSelector(state);
        const { applied: appliedSorters } = workspaceSel.sortersSelector(state);
        const appliedFacets = workspaceSel.appliedFacetsSelector(state);
        const { current, limit } = workspaceSel.paginationSelector(state);

        const queryParams = {
            ...appliedFilters,
            ...appliedSorters,
            page: current,
            limit: limit,
            f_ids: appliedFacets.map((o) => o.facetId),
            types: appliedFacets.map((o) => o.type),
            f_keys: appliedFacets.map((o) => o.keywordIds.join(ARRAY_DELIMITER)),
        };

        dispatch(routerOp.setQueryString(queryParams));
    };
};

/*
 * Setup Filters, Sorting, Pagination, Facets from Query string
 **/
const setupFiltersFromQs = (queryString: string) => {
    return (dispatch) => {
        try {
            const parsedQs = qs.parse(queryString, {
                parseArrays: true,
                comma: true,
                ignoreQueryPrefix: true,
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

            batchQueue.push(applyFilters(filters));

            // Facets
            if (parsedQs.f_ids) {
                const facetIds = parsedQs.f_ids;
                if (!Array.isArray(facetIds)) {
                    const fIds = facetIds.split(VALUE_DELIMITER);
                    const fValues = parsedQs.f_keys.split(VALUE_DELIMITER);
                    const fTypes = parsedQs.types.split(VALUE_DELIMITER);
                    fIds.forEach((fId, idx) => {
                        batchQueue.push(
                            applyFacet({
                                facet: {
                                    id: fId,
                                    type: fTypes[idx],
                                },
                                keywordIds: fValues[idx].split(ARRAY_DELIMITER),
                            })
                        );
                    });
                } else {
                    facetIds.forEach((facetId, i) => {
                        const facet: Partial<IFacetState> = {
                            id: facetId,
                            type: parsedQs.types[i],
                        };
                        batchQueue.push(
                            applyFacet({
                                facet,
                                keywordIds: parsedQs.f_keys[i].split(ARRAY_DELIMITER),
                            })
                        );
                    });
                }
            }

            // Pagination
            if (parsedQs.page) {
                batchQueue.push(changePage(+parsedQs.page));
            }

            //DropDown
            if (parsedQs.limit) {
                batchQueue.push(changeProjectsLimit(+parsedQs.limit));
            }

            // Sorting
            if (parsedQs.sortBy) {
                batchQueue.push(
                    applySorter({
                        sortBy: parsedQs.sortBy,
                        sortOrder: parsedQs.sortOrder,
                    })
                );
            }

            batch(() => batchQueue.forEach(dispatch));
        } catch {}
    };
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
        const { limit, offset } = selectors.paginationSelector(state);
        const appliedFilters = selectors.appliedFiltersSelector(state);
        const appliedFacets = selectors.appliedFacetsSelector(state);
        const sorters = selectors.sortersSelector(state);
        const projectId = commonSel.currentProjectIdSelector(state);

        const body: ISearchListRequest = {
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
        Object.keys(appliedFilters).forEach((filter) => {
            if (filter.length) {
                body[filter] = appliedFilters[filter];
            }
        });

        // get facets
        body.facets = appliedFacets.map((facet) => facet);

        try {
            const { total, facets, results, sortByProperties } = await requestSearchList(body);
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
            });
        } catch (e) {
            batch(() => {
                dispatch(setLoading(false));
                dispatch(setError(e));
            });
        }
    };
};

const fetchRemoveTaskAsync = (itemId: string, projectId?: string) => {
    return async (dispatch) => {
        batch(() => {
            dispatch(setLoading(true));
            dispatch(setError({}));
        });

        try {
            if (projectId) {
                await requestRemoveTask(itemId, projectId);
            } else {
                await requestRemoveProject(itemId);
            }

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
    };
};

const fetchCloneTaskAsync = (taskId: string, projectId: string, taskNewId: string) => {
    return async (dispatch) => {
        batch(() => {
            dispatch(setError({}));
            dispatch(setLoading(true));
        });

        try {
            await requestCloneTask(taskId, projectId, taskNewId);
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
    };
};

const fetchCreateTaskAsync = (formData: any, artefactId: string) => {
    return async (dispatch, getState) => {
        const currentProjectId = commonSel.currentProjectIdSelector(getState());
        const { label, description, ...restFormData } = formData;
        const metadata = {
            label,
            description,
        };

        const payload = {
            metadata,
            data: {
                // @FIXME: HARDCODED
                taskType: "Dataset",
                type: artefactId,
                parameters: {
                    ...restFormData,
                },
            },
        };

        dispatch(setError({}));

        try {
            const data = await requestCreateTask(payload, currentProjectId);

            dispatch(
                routerOp.goToPage(`/projects/${currentProjectId}/dataset/${data.id}`, {
                    taskLabel: label,
                })
            );
        } catch (e) {
            dispatch(setError(e));
        }
    };
};

const fetchCreateProjectAsync = (formData: { label: string; description?: string }) => {
    return async (dispatch) => {
        dispatch(setError({}));
        const { label, description } = formData;
        try {
            const data = await requestCreateProject({
                metaData: {
                    label,
                    description,
                },
            });
            dispatch(routerOp.goToPage(`/projects/${data.name}`, { projectLabel: label }));
        } catch (e) {
            dispatch(setError(e.response.data));
        }
    };
};

const applyFiltersOp = (filter) => {
    return (dispatch) => {
        batch(() => {
            dispatch(applyFilters(filter));
            dispatch(updateQueryString());
        });
    };
};

const applySorterOp = (sortBy: string) => {
    return (dispatch) => {
        batch(() => {
            dispatch(
                applySorter({
                    sortBy,
                })
            );
            dispatch(updateQueryString());
        });
    };
};

const changePageOp = (page: number) => {
    return (dispatch) => {
        batch(() => {
            dispatch(changePage(page));
            dispatch(updateQueryString());
        });
    };
};

const changeLimitOp = (value: number) => {
    return (dispatch) => {
        batch(() => {
            dispatch(changeProjectsLimit(value));
            dispatch(updateQueryString());
        });
    };
};

const toggleFacetOp = (facet: IFacetState, keywordId: string) => {
    return (dispatch, getState) => {
        const facets = workspaceSel.appliedFacetsSelector(getState());
        const foundFacet = facets.find((o) => o.facetId === facet.id);

        const isKeywordMissing = foundFacet && !foundFacet.keywordIds.includes(keywordId);

        if (!foundFacet || isKeywordMissing) {
            dispatch(
                applyFacet({
                    facet,
                    keywordIds: [keywordId],
                })
            );
        } else {
            dispatch(
                removeFacet({
                    facet: foundFacet,
                    keywordId,
                })
            );
        }

        dispatch(updateQueryString());
    };
};

export default {
    fetchListAsync,
    fetchRemoveTaskAsync,
    fetchCloneTaskAsync,
    applyFiltersOp,
    applySorterOp,
    changePageOp,
    changeLimitOp,
    toggleFacetOp,
    setupFiltersFromQs,
    fetchProjectPrefixesAsync,
    fetchAddOrUpdatePrefixAsync,
    fetchRemoveProjectPrefixAsync,
    fetchWarningListAsync,
    fetchWarningMarkdownAsync,
    fetchResourcesListAsync,
    checkIfResourceExistsAsync,
    fetchCreateProjectAsync,
    fetchCreateTaskAsync,
    resetFilters,
    updateNewPrefix,
};
