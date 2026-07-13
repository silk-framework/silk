import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";

import React from "react";
import { createRoot } from "react-dom/client";
import { Provider } from "react-redux";
import ErrorBoundary from "./app/ErrorBoundary";
import registerGlobalListeners from "./global";
import App from "./app/App";
import configs from "./configs";
import appRoutes, { IRouteProps } from "./app/appRoutes";
import { createPlugin } from "./app/services/pluginApi";
import configureStore from "./app/store/configureStore";

import "./theme/index.scss";
import "./theme/tailwind.generated.css";
import mappingEditor from "./app/views/pages/MappingEditor/index";
import "./language";

// Restyling experiment: dark-mode dev flag. The shadcn token set ships full dark values
// (`.dark` class variant), but legacy SCSS still hardcodes light colors, so this stays a
// dev-only switch until the SCSS sunset. Usage: `window.__toggleDarkMode()` in the console.
if (window.localStorage?.getItem("diapp-experimental-dark") === "true") {
    document.documentElement.classList.add("dark");
}
(window as any).__toggleDarkMode = (): boolean => {
    const dark = document.documentElement.classList.toggle("dark");
    window.localStorage?.setItem("diapp-experimental-dark", String(dark));
    return dark;
};

if (typeof mappingEditor.hierarchicalMapping !== "function") {
    console.error("Mapping editor factory methods no registered.");
}

// Prototype-pollution defense: `seal` blocks ADDING properties to Object.prototype
// (the pollution vector) while keeping the existing ones writable. A full `freeze`
// makes every inherited property non-writable, and strict mode then throws on any
// plain-object shadowing assignment like `obj.constructor = …` / `obj.toString = …`
// — a common, legitimate idiom in dependencies (d3-color, zod's v3 compat layer, …)
// that silently killed the workspacePlugins entry at startup.
Object.seal(Object.prototype);

const bootstrapPlugins = (plugins) => plugins.map((plugin) => createPlugin(plugin));

const bootstrapApp = (routes: IRouteProps[], externalRoutes) => {
    const store = configureStore(configs.dev);
    const rootDIv = document.getElementById("root");
    if (!rootDIv) return null;
    const root = createRoot(rootDIv);
    root.render(
        <ErrorBoundary>
            <Provider store={store}>
                <App routes={routes} externalRoutes={externalRoutes} />
            </Provider>
        </ErrorBoundary>,
    );
};

// @Note: Keep order of function
// Register Global Events and properties
registerGlobalListeners();
// Bootstrap plugins from settings.js
const pluginRoutes = bootstrapPlugins(configs.plugins);

// Bootstrap the React application
bootstrapApp(appRoutes, pluginRoutes);
