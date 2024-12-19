import React from "react";
import { Elements, OnLoadParams } from "react-flow-renderer";

/** Context for all UI related properties. */
export interface RuleEditorUiContextProps {
    /** If a modal is currently shown. This effects certain features like hot keys that get disabled when a modal is shown. */
    modalShown: boolean;
    /** Set flag that a modal is currently shown. */
    setModalShown: (shown: boolean) => any;
    /** If the advanced parameter mode is enabled. */
    advancedParameterModeEnabled: boolean;
    /** Enables/disables the advanced parameter mode. */
    setAdvancedParameterMode: (advancedEnabled: boolean) => any;
    reactFlowWrapper: React.MutableRefObject<any> | null;
    /** react-flow instance */
    setReactFlowInstance: (params: OnLoadParams) => any;
    /** Sets the react-flow instance, so it can be used everywhere in the view code. */
    reactFlowInstance: OnLoadParams | undefined;
    /** markdown description for rule operator node  */
    currentRuleNodeDescription: string | undefined;
    /** Sets the markdown description for rule operator node */
    setCurrentRuleNodeDescription: (description: string | undefined) => void;
    /** When enabled only the rule is shown without side- and toolbar and any other means to edit the rule. */
    showRuleOnly?: boolean;
    /** When enabled the mini map is not displayed. */
    hideMinimap?: boolean;
    /** Defines minimun and maximum of the available zoom levels */
    zoomRange?: [number, number];
    onSelection: (elements: Elements | null) => void;
    selectionState: { elements: Elements | null };
}

export const RuleEditorUiContext = React.createContext<RuleEditorUiContextProps>({
    modalShown: false,
    setModalShown(): any {},
    advancedParameterModeEnabled: false,
    setAdvancedParameterMode: () => {},
    reactFlowWrapper: null,
    setReactFlowInstance: () => {},
    reactFlowInstance: undefined,
    currentRuleNodeDescription: undefined,
    setCurrentRuleNodeDescription: () => {},
    showRuleOnly: false,
    hideMinimap: false,
    zoomRange: [0.25, 1.5],
    onSelection: () => {},
    selectionState: { elements: null },
});
