import React from "react";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableSettings, GlobalTableTypes,
    useStoreGlobalTableSettings
} from "./hooks/useStoreGlobalTableSettings";

export const GlobalContextsWrapper = ({children}) => {
    const {globalTableSettings, updateGlobalTableSettings} = useStoreGlobalTableSettings()
    return <GlobalTableContext.Provider value={{
        globalTableSettings,
        updateGlobalTableSettings,
    }}>
        {children}
    </GlobalTableContext.Provider>;
}

interface GlobalTableContextProps {
    globalTableSettings: GlobalTableSettings
    updateGlobalTableSettings: (settings: GlobalTableBaseConfig, explicitKey?: GlobalTableTypes) => void;
}

export const GlobalTableContext = React.createContext<GlobalTableContextProps>({
    globalTableSettings: defaultGlobalTableSettings,
    updateGlobalTableSettings: () => {}
});
