import { Elements, OnLoadParams } from "react-flow-renderer";
import React from "react";
import { IRuleOperator } from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";

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
    /** Save the current rule. */
    saveRule: () => Promise<boolean> | boolean;
    /** If there are unsaved changes. */
    unsavedChanges: boolean;
    /** Executes an operation that will change the model. */
    executeModelEditOperation: IModelActions;
    /** Undo last changes. Return true if changes have been undone. */
    undo: () => boolean;
    /** If there are changes that can be undone. */
    canUndo: boolean;
    /** Redo last undone changes. Return true if changes have been redone. */
    redo: () => boolean;
    /** If there are changes that can be redone. */
    canRedo: boolean;
}

export interface IModelActions {
    /** Starts a new change transaction. All actions after this will be handled as a single transaction, e.g. can be undone/redone as on operation. */
    startChangeTransaction: () => void;
    /** Add a rule operator as new rule node. */
    addNode: (ruleOperator: IRuleOperator, position: XYPosition) => void;
    /** Delete a rule node. */
    deleteNode: (nodeId: string) => void;
    /** Delete multiple rules nodes at once. */
    deleteNodes: (nodeIds: string[]) => void;
    /** Add an edge between two nodes. */
    addEdge: (sourceNodeId: string, targetNodeId: string, targetHandleId: string) => void;
    /** Delete an edge. */
    deleteEdge: (edgeId: string) => void;
    /** Copy and paste a selection of nodes. Move pasted selection by the defined offset. */
    copyAndPasteNodes: (nodeIds: string[], offset: XYPosition) => void;
    /** Move a single node to a new position. */
    moveNode: (nodeId: string, newPosition: XYPosition) => void;
    /** Change a single node parameter.
     *
     * @param nodeId Node affected by parameter change.
     * @param parameterId The parameter that is being changed.
     * @param newValue The new value of the parameter.
     * @param autoStartTransaction If this is true, new transactions are automatically started if the previous operation
     *                             was not changing the exact same parameter. Default is true.
     */
    changeNodeParameter: (
        nodeId: string,
        parameterId: string,
        newValue: string | undefined,
        autoStartTransaction?: boolean
    ) => void;
    /** Automatically layout the rule nodes. */
    autoLayout: () => void;
}

const NOP = () => {};

/** Creates a rule editor model context that contains the actual rule model and low-level update functions. */
export const RuleEditorModelContext = React.createContext<RuleEditorModelContextProps>({
    /** The nodes and edges of the rules graph. */
    elements: [],
    /** Set to true if the model is in read-only mode. */
    isReadOnly: false,
    /** Allows setting the model to read-only mode. */
    setIsReadOnly: NOP,
    setReactFlowInstance: NOP,
    saveRule: () => {
        return false;
    },
    unsavedChanges: false,
    executeModelEditOperation: {
        startChangeTransaction: NOP,
        deleteNode: NOP,
        deleteNodes: NOP,
        addNode: NOP,
        copyAndPasteNodes: NOP,
        moveNode: NOP,
        changeNodeParameter: NOP,
        addEdge: NOP,
        deleteEdge: NOP,
        autoLayout: NOP,
    },
    undo: () => false,
    canUndo: false,
    redo: () => false,
    canRedo: false,
});
