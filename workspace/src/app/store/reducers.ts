import common from "@ducks/common";
import error from "@ducks/error";
import routerReducers from "@ducks/router";
import workspace from "@ducks/workspace";
import { combineReducers } from "@reduxjs/toolkit";
import { Action, CombinedState, Reducer } from "redux";

import { IStore } from "./typings/IStore";

const reducers = (history): Reducer<CombinedState<IStore>, Action> => {
    return combineReducers({
        common: common.reducer,
        workspace,
        error: error,
        router: routerReducers(history),
    });
};

export default reducers;
