import * as types from "./types";
import { PreviewDto } from "./dtos";

const dashboardPreviewReducers = (state = new PreviewDto(), action: any = {}): PreviewDto => {
    switch (action.type) {
        case (types.FETCH_SEARCH_RESULTS):
            return {
                ...state,
                searchResults: [],
                isLoading: true
            };

        case (types.FETCH_SEARCH_RESULTS_SUCCESS):
            return {
                ...state,
                isLoading: false,
                searchResults: action.payload.results
            };

        case (types.FETCH_SEARCH_RESULTS_FAILURE):
            return {
                ...state,
                isLoading: false,
                error: action.payload.error
            };

        default:
            return state;
    }
};

export default dashboardPreviewReducers;
