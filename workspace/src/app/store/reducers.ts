import { combineReducers } from "@reduxjs/toolkit";

import workspace from "@ducks/workspace";
import globalSlice from "@ducks/global";
import dataset from "@ducks/dataset";
import routerReducers from "@ducks/router";

export default (history) => {
    return combineReducers({
        global: globalSlice.reducer,
        workspace,
        dataset: dataset.reducer,
        router: routerReducers(history)
    });
}


