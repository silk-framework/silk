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

        default:
            return state;
    }
};

export default global;
