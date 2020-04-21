import { combineReducers } from "@reduxjs/toolkit";

import workspace from "@ducks/workspace";
import common from "@ducks/common";
import dataset from "@ducks/dataset";
import routerReducers from "@ducks/router";

export default (history) => {
    return combineReducers({
        common: common.reducer,
        workspace,
        dataset: dataset.reducer,
        router: routerReducers(history)
    });
}


