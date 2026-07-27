import rootReducer from "./reducers";
import { configureStore } from "@reduxjs/toolkit";
import { createBrowserHistory } from "history";
import { routerMiddleware } from "connected-react-router";
import { getUserConfirmation } from "../views/shared/projectTaskTabView/unsavedChangesConfirmation";
import { createLogger } from "redux-logger";
import monitorReducerEnhancer from "./enhancers/monitorPerformanceEnhancer";
import storeDevEnhancer from "./enhancers/reduxDevEnhancer";
import React from "react";
import { isDevelopment } from "../constants/path";

let store;
// Custom getUserConfirmation lets the ProjectTaskTabView route its own history.block prompts
// through an in-app modal (opt-in per navigation); all other callers fall back to window.confirm.
let history = createBrowserHistory({ getUserConfirmation });

export const getStore = () => store;
export const getHistory = () => history;

export default function configStore(options: any = {}) {
    const enhancers: any[] = [];
    const middleware = [routerMiddleware(getHistory())];
    if (isDevelopment) {
        const { enableStoreDevUtils, monitorPerformance, logReduxActions, logUselessRenders } = options;
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
                collapsed: true,
            });
            middleware.push(logger);
        }

        if (logUselessRenders) {
            try {
                const whyDidYouRender = require("@welldone-software/why-did-you-render");
                whyDidYouRender(React, {
                    trackHooks: true,
                    trackAllPureComponents: true,
                    collapseGroups: true,
                    titleColor: "green",
                    exclude: [/^Blueprint/],
                    diffNameColor: "darkturquoise",
                    diffPathColor: "goldenrod",
                });
            } catch (e) {
                console.log(e);
            }
        }
    }

    store = configureStore({
        reducer: rootReducer(getHistory()),
        middleware: (getDefaultMiddleware) => getDefaultMiddleware({ serializableCheck: false }).concat(middleware),
        devTools: isDevelopment,
        enhancers: (defaultEnhancers) => defaultEnhancers().concat(enhancers),
    });

    return store;
}

export type AppDispatch = typeof store.dispatch;
export type RootState = ReturnType<typeof store.getState>;
