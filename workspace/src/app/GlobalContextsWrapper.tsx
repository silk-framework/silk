import React from "react";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableSettings,
    GlobalTableTypes,
    useStoreGlobalTableSettings,
} from "./hooks/useStoreGlobalTableSettings";
import { ModalContext, useModalContext } from "@eccenca/gui-elements/src/components/Dialog/ModalContext";

/** Wraps globally used contexts around the application component. */
export const GlobalContextsWrapper = ({ children }) => {
    const { globalTableSettings, updateGlobalTableSettings } = useStoreGlobalTableSettings();
    // Context values must keep their identity as long as they do not change, otherwise all consumers get re-rendered
    // whenever this component re-renders, e.g. the whole project page on every change of the open modal stack.
    const modalContextValue = useModalContext();
    const globalTableContextValue = React.useMemo(
        () => ({
            globalTableSettings,
            updateGlobalTableSettings,
        }),
        [globalTableSettings, updateGlobalTableSettings],
    );

    return (
        <GlobalTableContext.Provider value={globalTableContextValue}>
            <ModalContext.Provider value={modalContextValue}>{children}</ModalContext.Provider>
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
