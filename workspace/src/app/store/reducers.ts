import { combineReducers } from "@reduxjs/toolkit";

import workspace from "@ducks/workspace";
import common from "@ducks/common";
import routerReducers from "@ducks/router";
import { Reducer, Action } from "redux";
import { IStore } from "./typings/IStore";
import error from "@ducks/error";

const reducers = (history): Reducer<IStore, Action> => {
    return combineReducers({
        common: common.reducer,
        workspace,
        error: error,
        router: routerReducers(history),
    });
};

export default reducers;
