import React from "react";
import {OnLoadParams} from "react-flow-renderer";

/** Context for all UI related properties. */
export interface RuleEditorUiContextProps {
    /** If a modal is currently shown. This effects certain features like hot keys that get disabled when a modal is shown. */
    modalShown: boolean;
    /** Set flag that a modal is currently shown. */
    setModalShown: (shown: boolean) => any;
    reactFlowWrapper: React.MutableRefObject<any> | null;
    /** react-flow instance */
    setReactFlowInstance: (params: OnLoadParams) => any
    /** Sets the react-flow instance, so it can be used everywhere in the view code. */
    reactFlowInstance: OnLoadParams | undefined
}

export const RuleEditorUiContext = React.createContext<RuleEditorUiContextProps>({
    modalShown: false,
    setModalShown(): any {},
    reactFlowWrapper: null,
    setReactFlowInstance: () => {},
    reactFlowInstance: undefined
});
