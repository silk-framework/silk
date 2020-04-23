import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";
import ErrorBoundary from "./app/ErrorBoundary";
import registerGlobalListeners from "./global";
import App from "./app/App";
import configs from "./configs";
import appRoutes from "./app/appRoutes";
import { createPlugin } from "./app/services/pluginApi";
import configureStore from "./app/store/configureStore";

import "@wrappers/index.scss";
import "./language";

const bootstrapPlugins = (plugins) => plugins.map((plugin) => createPlugin(plugin));

const bootstrapApp = (routes, externalRoutes) => {
    const store = configureStore(configs.dev);
    ReactDOM.render(
        <ErrorBoundary>
            <Provider store={store}>
                <App routes={routes} externalRoutes={externalRoutes} />
            </Provider>
        </ErrorBoundary>,
        document.getElementById("root")
    );
};

// @Note: Keep order of function
// Register Global Events and properties
registerGlobalListeners();
// Bootstrap plugins from settings.js
const pluginRoutes = bootstrapPlugins(configs.plugins);

// Bootstrap the React application
bootstrapApp(appRoutes, pluginRoutes);
