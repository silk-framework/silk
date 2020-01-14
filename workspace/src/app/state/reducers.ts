import { combineReducers } from "@reduxjs/toolkit";

import dashboard from "@ducks/dashboard";
import { globalSlice } from "@ducks/global/globalSlice";
import routerReducers from "@ducks/router";
import { projectSlice } from "@ducks/project/projectSlice";

export default (history) => {
    return combineReducers({
        global: globalSlice.reducer,
        dashboard,
        project: projectSlice.reducer,
        router: routerReducers(history)
    });
}


