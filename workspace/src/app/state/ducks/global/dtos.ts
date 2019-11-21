import { isAuthenticated } from "./thunks/auth.thunk";
import { getLocale } from "./thunks/locale.thunk";

export class GlobalDto {
    locale: string = getLocale();
    authenticated: boolean = true;
    searchString: string = '';
    // authenticated: boolean = isAuthenticated();
}
