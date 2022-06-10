import React from "react";

/** Context for all UI related properties. */
export interface RuleEditorUiContextProps {
    /** If a modal is currently shown. This effects certain features like hot keys that get disabled when a modal is shown. */
    modalShown: boolean;
    /** Set flag that a modal is currently shown. */
    setModalShown: (shown: boolean) => any;
    showStickyNoteModal: boolean;
    setShowStickyNoteModal: (shown: boolean) => void;
}

export const RuleEditorUiContext = React.createContext<RuleEditorUiContextProps>({
    modalShown: false,
    setModalShown(): any {},
    showStickyNoteModal: false,
    setShowStickyNoteModal: () => {},
});
