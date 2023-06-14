import { batch } from "react-redux";

import selectors from "./selectors";
import { filtersSlice } from "./filtersSlice";
import { previewSlice } from "./previewSlice";
import { routerOp } from "@ducks/router";
import { IFacetState } from "@ducks/workspace/typings";
import { workspaceSel } from "@ducks/workspace";
import qs, { ParsedQs } from "qs";
import {
    fetchAddOrUpdatePrefixAsync,
    fetchProjectPrefixesAsync,
    fetchRemoveProjectPrefixAsync,
} from "@ducks/workspace/widgets/configuration.thunk";
import { widgetsSlice } from "@ducks/workspace/widgetsSlice";
import { fetchWarningListAsync, fetchWarningMarkdownAsync } from "@ducks/workspace/widgets/warning.thunk";
import { fetchResourcesListAsync } from "@ducks/workspace/widgets/file.thunk";
import { commonSel } from "@ducks/common";
import { ISearchListRequest, ISearchListResponse, requestSearchList } from "@ducks/workspace/requests";
import { FetchResponse } from "../../../services/fetch/responseInterceptor";

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
        //console.log("Applied sorters", appliedSorters);
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
            const parsedQs: any = qs.parse(queryString, {
                parseArrays: true,
                comma: true,
                ignoreQueryPrefix: true,
            });

            // The batch of functions that should dispatched
            const batchQueue: { payload: any; type: string }[] = [];

            // setup filters
            const filters: any = Object.create(null);
            if (parsedQs.textQuery) {
                filters.textQuery = parsedQs.textQuery;
            }
            if (parsedQs.itemType) {
                filters.itemType = parsedQs.itemType;
            }

            batchQueue.push(applyFilters(filters));

            /** Only expect string or array of strings. */
            const toStringArray = (value: undefined | string | string[] | ParsedQs | ParsedQs[]): string[] => {
                if (Array.isArray(value) && value.length > 0 && typeof value[0] === "string") {
                    return value as string[];
                } else if (typeof value === "string") {
                    return value.split(VALUE_DELIMITER);
                } else {
                    return [];
                }
            };

            // Facets
            if (parsedQs.f_ids) {
                const facetIds = parsedQs.f_ids;
                const types = toStringArray(parsedQs.types);
                const fKeys = toStringArray(parsedQs.f_keys);
                if (typeof facetIds === "string") {
                    const fIds = toStringArray(facetIds);
                    const fValues = toStringArray(parsedQs.f_keys);
                    const fTypes = toStringArray(parsedQs.types);
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
                    toStringArray(facetIds).forEach((facetId, i) => {
                        const facet: Partial<IFacetState> = {
                            id: facetId,
                            type: types[i],
                        };
                        batchQueue.push(
                            applyFacet({
                                facet,
                                keywordIds: fKeys[i].split(ARRAY_DELIMITER),
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
const fetchListAsync = (
    fetcher?: (payload: ISearchListRequest) => Promise<FetchResponse<ISearchListResponse>>,
    customDefaultLimit?: number
) => {
    return async (dispatch, getState) => {
        dispatch(fetchList());

        batch(() => {
            dispatch(setError({}));
            dispatch(setLoading(true));
        });
        // get applied pagination values
        const state = getState();
        const { limit, offset } = selectors.paginationSelector(state);
        const appliedFilters = selectors.appliedFiltersSelector(state);
        const appliedFacets = selectors.appliedFacetsSelector(state);
        const sorters = selectors.sortersSelector(state);
        const projectId = commonSel.currentProjectIdSelector(state);

        const body: ISearchListRequest = {
            limit: customDefaultLimit || limit,
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
            const { total, facets, results, sortByProperties } = fetcher
                ? (await fetcher(body))?.data
                : await requestSearchList(body);
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
                dispatch(setError(e));
            });
        } finally {
            dispatch(setLoading(false));
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

const workspaceOps = {
    fetchListAsync,
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
    resetFilters,
    updateNewPrefix,
    applyFilters,
    changeProjectsLimit,
};

export default workspaceOps;
