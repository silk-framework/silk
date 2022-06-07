import React from "react";

/** Context for all UI related properties. */
export interface RuleEditorUiContextProps {
    /** If a modal is currently shown. This effects certain features like hot keys that get disabled when a modal is shown. */
    modalShown: boolean;
    /** Set flag that a modal is currently shown. */
    setModalShown: (shown: boolean) => any;
    /** If the advanced parameter mode is enabled. */
    advancedParameterModeEnabled: boolean
    /** Enables/disables the advanced parameter mode. */
    setAdvancedParameterMode: (advancedEnabled: boolean) => any
}

export const RuleEditorUiContext = React.createContext<RuleEditorUiContextProps>({
    modalShown: false,
    setModalShown(): any {},
    advancedParameterModeEnabled: false,
    setAdvancedParameterMode: () => {}
});
