import { Elements, OnLoadParams } from "react-flow-renderer";
import React from "react";

/**
 * The rule editor model context that contains objects and methods related to the rule model of the editor, i.e.
 * of the underlying nodes and edges that are displayed in the visual editor.
 *
 * @param ITEM_TYPE The interface of the rule based item that is being edited.
 * @param OPERATOR_TYPE The interface of the operators that can be placed in the editor.
 */
export interface RuleEditorModelContextProps {
    /** The rule nodes and edges. */
    elements: Elements;
    /** If the model is set to read-only. */
    isReadOnly: boolean;
    /** Sets the read-only mode of the model. */
    setIsReadOnly: (readOnly: boolean) => any;
    /** Callback to set the react-flow instance needed for the model. */
    setReactFlowInstance: (instance: OnLoadParams) => any;
}

/** Creates a rule editor model context that contains the actual rule model and low-level update functions. */
export const RuleEditorModelContext = React.createContext<RuleEditorModelContextProps>({
    /** The nodes and edges of the rules graph. */
    elements: [],
    /** Set to true if the model is in read-only mode. */
    isReadOnly: false,
    /** Allows setting the model to read-only mode. */
    setIsReadOnly: () => {},
    setReactFlowInstance: () => {},
});
