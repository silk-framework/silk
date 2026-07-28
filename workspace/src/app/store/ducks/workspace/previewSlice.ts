import { ActionReducerMapBuilder, createAction, createSlice, WritableDraft } from "@reduxjs/toolkit";
import { LOCATION_CHANGE } from "connected-react-router";
import { initialPreviewState } from "./initialState";
import { getHistory } from "../../configureStore";
import { IPreviewState } from "./typings";

/**
 * The search-result list is shared by several pages (workbench list, project contents,
 * activities) whose result items have different shapes. Clear it whenever the route's
 * pathname changes, so the next page never renders the previous page's results during
 * its first render (activity items e.g. have no `type` and crash the item table).
 * Query-string-only changes (pagination, facets) keep the results while re-fetching.
 */
const getExtraReducers = (builder: ActionReducerMapBuilder<WritableDraft<IPreviewState>>) => {
    builder.addCase(createAction(LOCATION_CHANGE).toString(), (state) => {
        const { pathname } = getHistory().location;
        if (state.resultsPathname !== pathname) {
            state.resultsPathname = pathname;
            state.searchResults = [];
        }
    });
};

export const previewSlice = createSlice({
    name: "preview",
    initialState: initialPreviewState(),
    reducers: {
        clearSearchResults(state) {
            state.searchResults = [];
        },
        setLoading(state, action) {
            state.isLoading = action.payload;
        },
        setError(state, action) {
            state.error = action.payload;
        },
        fetchList(state) {
            state.searchResults.length = 0;
        },
        fetchListSuccess(state, action) {
            state.searchResults = action.payload;
        },
    },
    extraReducers: getExtraReducers,
});
