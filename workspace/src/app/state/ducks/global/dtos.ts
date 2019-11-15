import { isAuthenticated } from "./thunks/auth.thunk";
import { getLocale } from "./thunks/locale.thunk";

export class GlobalDto {
    locale: string = getLocale();
    authenticated: boolean = true;
    // authenticated: boolean = isAuthenticated();
}
