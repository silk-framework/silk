import { getLocale } from "./thunks/locale.thunk";

export interface IGlobalState {
    locale: string;
    authenticated: boolean;
    searchQuery: string;
    loading: boolean;
    // @TODO: add the typos for the last 2 properties
    // searchResults: any[] = [];
    error: any;
    // authenticated: boolean = isAuthenticated();
}

export function initialGlobalState(): IGlobalState {
    return {
        locale: getLocale(),
        authenticated: true,
        searchQuery: '',
        loading: false,
        error: {},
        // authenticated: boolean = isAuthenticated();
    }
}
