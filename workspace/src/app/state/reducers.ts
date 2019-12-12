import { combineReducers } from "@reduxjs/toolkit";
import { connectRouter } from 'connected-react-router'

import dashboard from "./ducks/dashboard";
import { globalSlice } from "./ducks/global/globalSlice";

export default (history) => {
    return combineReducers({
        global: globalSlice.reducer,
        dashboard,
        router: connectRouter(history)
    });
}


