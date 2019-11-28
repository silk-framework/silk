import { isAuthenticated } from "./thunks/auth.thunk";
import { getLocale } from "./thunks/locale.thunk";

export class GlobalDto {
    locale: string = getLocale();
    authenticated: boolean = true;
    searchQuery: string = '';
    loading: boolean = false;
    // @TODO: add the typos for the last 2 properties
    // searchResults: any[] = [];
    error: any = {};
    // authenticated: boolean = isAuthenticated();
}
