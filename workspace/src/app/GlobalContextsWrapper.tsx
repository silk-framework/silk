import React from "react";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableSettings,
    GlobalTableTypes,
    useStoreGlobalTableSettings,
} from "./hooks/useStoreGlobalTableSettings";
import { ModalContext, useModalContext } from "@eccenca/gui-elements/src/components/Dialog/ModalContext";
import { ApplicationContextRegistryProvider } from "./contexts/ApplicationContextRegistry";

/** Wraps globally used contexts around the application component. */
export const GlobalContextsWrapper = ({ children }) => {
    const { globalTableSettings, updateGlobalTableSettings } = useStoreGlobalTableSettings();
    const { openModalStack, setModalOpen } = useModalContext();

    return (
        <GlobalTableContext.Provider
            value={{
                globalTableSettings,
                updateGlobalTableSettings,
            }}
        >
            <ModalContext.Provider value={{ openModalStack, setModalOpen }}>
                <ApplicationContextRegistryProvider>{children}</ApplicationContextRegistryProvider>
            </ModalContext.Provider>
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
