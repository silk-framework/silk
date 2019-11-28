import { combineReducers } from "redux-starter-kit";
import { connectRouter } from 'connected-react-router'

import global from './ducks/global';
import dashboard from "./ducks/dashboard";

export default (history) => {
    return combineReducers({
        global,
        dashboard,
        router: connectRouter(history)
    });
}


