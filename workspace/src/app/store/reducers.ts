import { combineReducers } from "@reduxjs/toolkit";

import dashboard from "@ducks/workspace";
import { globalSlice } from "@ducks/global/globalSlice";
import routerReducers from "@ducks/router";

export default (history) => {
    return combineReducers({
        global: globalSlice.reducer,
        dashboard,
        router: routerReducers(history)
    });
}


