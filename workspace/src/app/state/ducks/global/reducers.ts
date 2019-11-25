import * as types from "./types";
import { GlobalDto } from "./dtos";

const global = (state = new GlobalDto(), action: any = {}): GlobalDto => {
    switch (action.type) {
        case (types.CHANGE_LANGUAGE):
            return {
                ...state,
                locale: action.payload.locale
            };

        case (types.LOGIN_SUCCESS):
            return {
                ...state,
                authenticated: true,
            };

        case (types.LOG_OUT):
            return {
                ...state,
                authenticated: false,
            };

        case (types.CHANGE_SEARCH_STRING):
            return {
                ...state,
                searchString: action.payload.searchString
            };

        case (types.FETCH_SEARCH_RESUTLS):
            return {
                ...state,
                loading: true
            };

        case (types.SEARCH_RESUTLS_SUCCESS):
            return {
                ...state,
                searchResults: action.payload.data.results,
                loading: false
            };

        case (types.SEARCH_RESUTLS_FAILURE):
            return {
                ...state,
                error: action.payload.error,
                loading: false
            };

        default:
            return state;
    }
};

export default global;
