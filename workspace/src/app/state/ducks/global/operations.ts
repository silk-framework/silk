import { authorize, isAuthenticated, getTokenFromStore, logout } from "./thunks/auth.thunk";
import { changeLocale } from "./thunks/locale.thunk";


export default {
    changeLocale,
    isAuthenticated,
    getTokenFromStore,
    authorize,
    logout,
};
