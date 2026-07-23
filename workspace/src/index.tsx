import "react-app-polyfill/ie11";
import "react-app-polyfill/stable";

import React from "react";
import { createRoot } from "react-dom/client";
import { ThemeProvider } from "next-themes";
import { Provider } from "react-redux";
import { shadcn } from "@eccenca/gui-elements";
import ErrorBoundary from "./app/ErrorBoundary";
import registerGlobalListeners from "./global";
import App from "./app/App";
import configs from "./configs";
import appRoutes, { IRouteProps } from "./app/appRoutes";
import { createPlugin } from "./app/services/pluginApi";
import configureStore from "./app/store/configureStore";

// compiled legacy styles: gui-elements bundle first, then the app sheets (former theme/index.scss)
import "@eccenca/gui-elements/src/css/index.css";
import "./theme/index.css";
import "./theme/tailwind.generated.css";
// Loaded AFTER the Tailwind output so it can override the buggy false-active sidebar fill.
import "./theme/sidebar-active-fix.css";
// GridBoard tile-fill rule (self-carding widget cards stretch to fill their resizable tile).
import "./theme/gridboard.css";
// Lifts the quick search dialog above the high z-index application header while it is open.
import "./theme/quicksearch.css";
import mappingEditor from "./app/views/pages/MappingEditor/index";
import "./language";

// Restyling experiment: dark-mode dev flag. The shadcn token set ships full dark values
// (`.dark` class variant), but legacy SCSS still hardcodes light colors, so this stays a
// dev-only switch until the SCSS sunset. Usage: `window.__toggleDarkMode()` in the console.
// The `dark` class on <html> is owned by next-themes (`ThemeProvider attribute="class"`,
// storage key `diapp-theme`) — the pristine shadcn `sonner.tsx` reads the theme through
// next-themes' `useTheme`. The toggle flips the class in place and persists the choice.
const legacyDarkFlag = window.localStorage?.getItem("diapp-experimental-dark") === "true";
// The dark-mode experiment persists `diapp-theme` in localStorage. A value left over from an
// earlier session strands the shadcn sidebar in dark (it honours `.dark`) while the legacy
// light-SCSS shell stays light — a jarring dark-gray sidebar on an otherwise light app. Unless
// the legacy dark flag is explicitly set, reset to light on boot so a stale preference can't
// keep us in dark. `__toggleDarkMode()` still works within a session.
if (!legacyDarkFlag) {
    window.localStorage?.removeItem("diapp-theme");
    document.documentElement.classList.remove("dark");
}
(window as any).__toggleDarkMode = (): boolean => {
    const dark = document.documentElement.classList.toggle("dark");
    window.localStorage?.setItem("diapp-theme", dark ? "dark" : "light");
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
            <ThemeProvider
                attribute="class"
                storageKey="diapp-theme"
                defaultTheme={legacyDarkFlag ? "dark" : "light"}
                enableSystem={false}
            >
                {/* The pristine shadcn sidebar/tooltip primitives expect an app-level
                    TooltipProvider (the radix-nova sidebar no longer mounts its own). */}
                <shadcn.TooltipProvider>
                    <Provider store={store}>
                        <App routes={routes} externalRoutes={externalRoutes} />
                    </Provider>
                </shadcn.TooltipProvider>
            </ThemeProvider>
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
