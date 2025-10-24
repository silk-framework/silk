import React from "react";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableSettings,
    GlobalTableTypes,
    useStoreGlobalTableSettings,
} from "./hooks/useStoreGlobalTableSettings";

/** Wraps globally used contexts around the application component. */
export const GlobalContextsWrapper = ({ children }) => {
    const { globalTableSettings, updateGlobalTableSettings } = useStoreGlobalTableSettings();
    return (
        <GlobalTableContext.Provider
            value={{
                globalTableSettings,
                updateGlobalTableSettings,
            }}
        >
            {children}
        </GlobalTableContext.Provider>
    );
};

interface GlobalTableContextProps {
    globalTableSettings: GlobalTableSettings;
    updateGlobalTableSettings: (settings: GlobalTableBaseConfig, explicitKey?: GlobalTableTypes) => void;
}

/** Context that provides properties for the persisted global table configuration. */
export const GlobalTableContext = React.createContext<GlobalTableContextProps>({
    globalTableSettings: defaultGlobalTableSettings,
    updateGlobalTableSettings: () => {},
});
