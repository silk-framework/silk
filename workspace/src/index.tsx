import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";
import "./theme/index.scss";
import "./language";

import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";

import App from "./app/App";
import appRoutes, { IRouteProps } from "./app/appRoutes";
import ErrorBoundary from "./app/ErrorBoundary";
import { createPlugin } from "./app/services/pluginApi";
import configureStore from "./app/store/configureStore";
import mappingEditor from "./app/views/pages/MappingEditor/index";
import configs from "./configs";
import registerGlobalListeners from "./global";

if (typeof mappingEditor.hierarchicalMapping !== "function") {
    console.error("Mapping editor factory methods no registered.");
}

const bootstrapPlugins = (plugins) => plugins.map((plugin) => createPlugin(plugin));

const bootstrapApp = (routes: IRouteProps[], externalRoutes) => {
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
