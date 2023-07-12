import { Elements, OnLoadParams } from "react-flow-renderer";
import React from "react";
import { IRuleOperator, IRuleOperatorNode, RuleOperatorNodeParameters } from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import { NodeDimensions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeContent";

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
    isReadOnly(): boolean;
    /** Same as above. Should be used where state change is pushed into the component. */
    readOnly: boolean;
    /** Sets the read-only mode of the model. If the function is not defined, read-only mode cannot be changed. */
    setIsReadOnly?: (readOnly: boolean) => any;
    /** Callback to set the react-flow instance needed for the model. */
    setReactFlowInstance: React.Dispatch<React.SetStateAction<OnLoadParams<any> | undefined>>;
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
    /** Returns true if this is a valid edge, false otherwise. */
    isValidEdge: (sourceNodeId: string, targetNodeId: string, targetHandleId: string) => boolean;
    /** Center the node having the given ID.
     * Returns true if the node has been found and centered, else false. */
    centerNode: (nodeId: string) => boolean;
    /** Get the current rule as IRuleOperatorNode objects. */
    ruleOperatorNodes: (pristineElements?: Elements) => IRuleOperatorNode[];
    /* from the focus node find all connected nodes that form the subtree*/
    getSubTreeNodes: (nodeId: string) => Elements;
    /** The ID of the rule editor canvas element. */
    canvasId: string;
}

export interface IModelActions {
    /** Starts a new change transaction. All actions after this will be handled as a single transaction, e.g. can be undone/redone as on operation. */
    startChangeTransaction: () => void;
    /** Add sticky note node as new node */
    addStickyNode: (stickyNote: string, position: XYPosition, color: string) => void;
    /** Add a rule operator as new rule node. */
    addNode: (ruleOperator: IRuleOperator, position: XYPosition) => void;
    /** Add node by plugin type and ID. */
    addNodeByPlugin: (
        pluginType: string,
        pluginId: string,
        position: XYPosition,
        overwriteParameterValues?: RuleOperatorNodeParameters,
        isCanvasPosition?: boolean
    ) => void;
    /** Delete a rule node. */
    deleteNode: (nodeId: string) => void;
    /** Delete multiple rules nodes at once. */
    deleteNodes: (nodeIds: string[]) => void;
    /** Add an edge between two nodes. */
    addEdge: (
        sourceNodeId: string,
        targetNodeId: string,
        // If target handle is undefined, connect to the first free handle
        targetHandleId: string | undefined,
        previousTargetHandle?: string
    ) => void;
    /** Delete an edge.
     *
     * @param edgeId        The ID of the edge that should be deleted.
     * @param updateHandles If the handles of the target node should be updated after this operation.
     */
    deleteEdge: (edgeId: string, updateHandles?: boolean) => void;
    /** Delete multiple edges */
    deleteEdges: (edgeIds: string[]) => void;
    /** Copy and paste a selection of nodes. Move pasted selection by the defined offset. */
    copyAndPasteNodes: (nodeIds: string[], offset?: XYPosition) => void;
    /** Move a single node to a new position. */
    moveNode: (nodeId: string, newPosition: XYPosition) => void;
    /** changes the size of a node to the given new dimensions */
    changeSize: (nodeId: string, newNodeDimension: NodeDimensions) => void;
    /** changes stickyNode properties such as style and content */
    changeStickyNodeProperties: (nodeId: string, color?: string, content?: string) => void;
    /** Moves nodes by a specific offset. */
    moveNodes: (nodeIds: string[], offset: XYPosition) => void;
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
    autoLayout: (startTransaction?: boolean) => void;
    /** Checks all nodes if their input ports need to be adapted, i.e. their number decreased or increased. Usually this is not needed and all operations are handling it themselves.
     * E.g. the deleteEdge function has the option to not fix affected nodes after deleting an edge. */
    fixNodeInputs: () => void;
}

const NOP = () => {};

/** Creates a rule editor model context that contains the actual rule model and low-level update functions. */
export const RuleEditorModelContext = React.createContext<RuleEditorModelContextProps>({
    /** The nodes and edges of the rules graph. */
    elements: [],
    /** Set to true if the model is in read-only mode. */
    isReadOnly: () => false,
    readOnly: false,
    setReactFlowInstance: NOP,
    saveRule: () => {
        return false;
    },
    unsavedChanges: false,
    executeModelEditOperation: {
        startChangeTransaction: NOP,
        addStickyNode: NOP,
        deleteNode: NOP,
        deleteNodes: NOP,
        addNode: NOP,
        copyAndPasteNodes: NOP,
        moveNode: NOP,
        moveNodes: NOP,
        changeNodeParameter: NOP,
        addEdge: NOP,
        deleteEdge: NOP,
        autoLayout: NOP,
        addNodeByPlugin: NOP,
        deleteEdges: NOP,
        changeSize: NOP,
        fixNodeInputs: NOP,
        changeStickyNodeProperties: NOP,
    },
    undo: () => false,
    canUndo: false,
    redo: () => false,
    canRedo: false,
    isValidEdge: () => true,
    centerNode: () => true,
    ruleOperatorNodes: () => [],
    getSubTreeNodes: () => [],
    canvasId: "canvasId",
});
