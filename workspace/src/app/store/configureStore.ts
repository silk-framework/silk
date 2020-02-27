import rootReducer from './reducers';
import { configureStore, getDefaultMiddleware } from "@reduxjs/toolkit";
import { createBrowserHistory } from 'history';
import { routerMiddleware } from "connected-react-router";
import { createLogger } from 'redux-logger'
import { isDevelopment } from "../constants";
import monitorReducerEnhancer from "./enhancers/monitorPerformanceEnhancer";
import storeDevEnhancer from "./enhancers/reduxDevEnhancer";

let store;
let history = createBrowserHistory();

export const getStore = () => store;
export const getHistory = () => history;

export default function (options: any = {}) {
    const enhancers = [];
    const middleware = [
        ...getDefaultMiddleware({
            serializableCheck: false,
        }),
        routerMiddleware(getHistory()),
    ];
    if (isDevelopment) {
        const {enableStoreDevUtils, monitorPerformance, logReduxActions} = options;
        // Enable redux development actions, e.g. reset store
        if (enableStoreDevUtils) {
            enhancers.push(storeDevEnhancer);
        }
        // Enable redux performance logger from settings
        if (monitorPerformance) {
            enhancers.push(monitorReducerEnhancer);
        }
        if (logReduxActions) {
            const logger = createLogger({
                collapsed: true
            });
            middleware.push(logger);
        }
    }

    store = configureStore({
        reducer: rootReducer(getHistory()),
        middleware,
        devTools: isDevelopment,
        enhancers
    });


    return store;
}

