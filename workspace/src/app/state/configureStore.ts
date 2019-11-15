import rootReducer from './reducers';
import { configureStore, getDefaultMiddleware } from "redux-starter-kit";
import { createBrowserHistory } from 'history';
import { routerMiddleware } from "connected-react-router";
import logger from 'redux-logger'
import { isDevelopment } from "../constants";
import monitorReducerEnhancer from "./enhancers/monitorPerformanceEnhancer";
import storeDevEnhancer from "./enhancers/reduxDevEnhancer";

let store;

export const getStore = () => store;

export const history = createBrowserHistory();

export default function (options: any = {}) {
    const enhancers = [];
    if (isDevelopment) {
        const {enableStoreDevUtils, monitorPerformance} = options;
        // Enable redux development actions, e.g. reset store
        if (enableStoreDevUtils) {
            enhancers.push(storeDevEnhancer);
        }
        // Enable redux performance logger from settings
        if (monitorPerformance) {
            enhancers.push(monitorReducerEnhancer);
        }
    }

    store = configureStore({
        reducer: rootReducer(history),
        middleware: [
            ...getDefaultMiddleware({
                serializableCheck: false
            }),
            logger,
            routerMiddleware(history),
        ],
        devTools: isDevelopment,
        enhancers
    });

    return store;
}

