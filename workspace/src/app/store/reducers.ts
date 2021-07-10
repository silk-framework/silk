import { combineReducers } from "@reduxjs/toolkit";

import workspace from "@ducks/workspace";
import common from "@ducks/common";
import dataset from "@ducks/dataset";
import routerReducers from "@ducks/router";
import { Reducer, Action, CombinedState } from "redux";
import { IStore } from "./typings/IStore";
import error from "@ducks/error";

export default (history): Reducer<CombinedState<IStore>, Action> => {
    return combineReducers({
        common: common.reducer,
        workspace,
        error: error,
        dataset: dataset.reducer,
        router: routerReducers(history),
    });
};
