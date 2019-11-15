import { combineReducers } from "redux-starter-kit";
import { connectRouter } from 'connected-react-router'

import global from './ducks/global';

export default (history) => {
    return combineReducers({
        global,
        router: connectRouter(history)
    });
}


