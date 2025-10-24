import React from "react";
import {
    defaultGlobalTableSettings,
    GlobalTableBaseConfig,
    GlobalTableSettings,
    GlobalTableTypes,
    useStoreGlobalTableSettings
} from "./hooks/useStoreGlobalTableSettings";
import {ModalContext, ModalContextProps} from "@eccenca/gui-elements/src/components/Dialog/ModalContext";

/** Wraps globally used contexts around the application component. */
export const GlobalContextsWrapper = ({children}) => {
    const {globalTableSettings, updateGlobalTableSettings} = useStoreGlobalTableSettings()
    const {openModalStack, setModalOpen} = useModalContext()

    return <GlobalTableContext.Provider value={{
        globalTableSettings,
        updateGlobalTableSettings,
    }}>
        <ModalContext.Provider value={{openModalStack, setModalOpen}}>
            {children}
        </ModalContext.Provider>
    </GlobalTableContext.Provider>;
}

interface GlobalTableContextProps {
    globalTableSettings: GlobalTableSettings
    updateGlobalTableSettings: (settings: GlobalTableBaseConfig, explicitKey?: GlobalTableTypes) => void;
}

/** Context that provides properties for the persisted global table configuration. */
export const GlobalTableContext = React.createContext<GlobalTableContextProps>({
    globalTableSettings: defaultGlobalTableSettings,
    updateGlobalTableSettings: () => {}
});

const useModalContext = (): ModalContextProps => {
    // A stack of modal IDs. These should reflect a stacked opening of modals on top of each other.
    const [openModalStack, setOpenModalStack] = React.useState<string[]>([])

    const setModalOpen = React.useCallback((modalId: string, isOpen: boolean) => {
        setOpenModalStack(old => {
            if(isOpen) {
                return [...old, modalId]
            } else {
                const idx = old.findIndex(id => modalId === id)
                switch(idx) {
                    case -1:
                        console.warn(`Trying to close modal with ID '${modalId}' that has not been registered as open!`)
                        return old;
                    case old.length - 1:
                        return old.slice(0, idx)
                    default:
                        // Modal in between is closed. Consider all modals after it also as closed and log warning
                        console.warn(`Modal with ID '${modalId}' is closed. Several modals opened after it are also considered as closed. IDs: ${old.slice(idx + 1).join(", ")}`)
                        return old.slice(0, idx)
                }
            }
        })
    }, [])

    return {
        openModalStack: openModalStack.length ? [...openModalStack] : undefined,
        setModalOpen
    }
}
