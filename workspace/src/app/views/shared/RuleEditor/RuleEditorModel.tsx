import React from "react";
import { Edge, Elements, FlowElement, Node, OnLoadParams, removeElements } from "react-flow-renderer";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "./contexts/RuleEditorContext";
import ruleEditorUtils from "./RuleEditor.utils";
import { useTranslation } from "react-i18next";
import {
    DeleteNodeAction,
    IRuleModelEditAction,
    IRuleNodeData,
    NodeContentPropsWithBusinessData,
} from "./RuleEditor.typings";
import { ChangeNodeParameters, RuleModelChanges, RuleModelChangeType } from "./RuleEditorModel.typings";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

type RuleEditorNode = Node<NodeContentPropsWithBusinessData<IRuleNodeData>>;

// Object to denote transaction boundaries between change operations
const TRANSACTION_BOUNDARY = "Transaction boundary";
type ChangeStackType = RuleModelChanges | "Transaction boundary";

/** The actual rule model, i.e. the model that is displayed in the editor.
 *  All rule model changes must happen here. */
export const RuleEditorModel = <ITEM_TYPE extends object>({ children }: RuleEditorModelProps) => {
    const { t } = useTranslation();
    /** If set, then the model cannot be modified. */
    const [isReadOnly, setIsReadOnly] = React.useState<boolean>(false);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    /** The nodes and edges of the rule editor. */
    const [elements, setElements] = React.useState<Elements>([]);
    // Contexts
    const ruleEditorContext = React.useContext<RuleEditorContextProps>(RuleEditorContext);
    const [ruleUndoStack, setRuleUndoStack] = React.useState<ChangeStackType[]>([]);
    const [canUndo, setCanUndo] = React.useState<boolean>(false);
    const [ruleRedoStack, setRuleRedoStack] = React.useState<ChangeStackType[]>([]);
    const [canRedo, setCanRedo] = React.useState<boolean>(false);

    /**
     * UNDO/REDO handling.
     **/

    /** Starts a new change transaction than can be undone/redone in one step. */
    const startChangeTransaction = () => {
        ruleUndoStack.push(TRANSACTION_BOUNDARY);
    };

    const startRedoChangeTransaction = (): void => {
        ruleRedoStack.push(TRANSACTION_BOUNDARY);
    };

    /** Returns inverted model change operations, i.e. that will revert the changes done by the provided changes. */
    const invertModelChanges = (ruleModelChange: RuleModelChanges): RuleModelChanges => {
        const invertedModelChanges = [...ruleModelChange.operations].reverse().map((op) => revertedModelChange(op));
        return { operations: invertedModelChanges };
    };

    /** Returns a reverted model change operation, i.e. that rolls back the change. */
    const revertedModelChange = (undoOp: RuleModelChangeType): RuleModelChangeType => {
        switch (undoOp.type) {
            case "Add node":
                return { type: "Delete node", node: undoOp.node };
            case "Delete node":
                return { type: "Add node", node: undoOp.node };
            case "Change node parameter":
                return {
                    type: "Change node parameter",
                    nodeId: undoOp.nodeId,
                    parameterId: undoOp.parameterId,
                    from: undoOp.to,
                    to: undoOp.from,
                };
            case "Change node position":
                return { type: "Change node position", nodeId: undoOp.nodeId, from: undoOp.to, to: undoOp.from };
            case "Add edge":
                return { type: "Delete edge", edge: undoOp.edge };
            case "Delete edge":
                return { type: "Add edge", edge: undoOp.edge };
        }
    };

    /** Undo the last change transaction. */
    const undo = (): boolean => {
        const changesToUndo = changeTransactionOperations(ruleUndoStack);
        if (changesToUndo.length > 0) {
            // Undo changes and create redo transaction
            startRedoChangeTransaction();
            // Changes must be redone in the reversed order
            const redoChanges = [...changesToUndo].reverse();
            redoChanges.forEach((change) => addRedoRuleModelChange(invertModelChanges(change)));
            changesToUndo.forEach((change) => executeRuleModelChange(change));
        }
        if (ruleUndoStack.length === 0) {
            setCanUndo(false);
        }
        return changesToUndo.length > 0;
    };

    /** Redo the last change transaction. */
    const redo = () => {
        const changesToRedo = changeTransactionOperations(ruleRedoStack);
        if (changesToRedo.length > 0) {
            // Redo changes and create Undo transaction
            startChangeTransaction();
            // Changes must be redone in the reversed order
            const undoChanges = [...changesToRedo].reverse();
            undoChanges.forEach((change) => addRuleModelChange(invertModelChanges(change)));
            changesToRedo.forEach((change) => executeRuleModelChange(change));
        }
        if (ruleRedoStack.length === 0) {
            setCanRedo(false);
        }
        return changesToRedo.length > 0;
    };

    /** Returns the last change transaction of the given change stack. */
    const changeTransactionOperations = (changeStack: ChangeStackType[]) => {
        // Find first real operation
        let op = changeStack.pop();
        while (changeStack.length > 0 && op === TRANSACTION_BOUNDARY) {
            op = changeStack.pop();
        }
        // Add all operations until transaction boundary is hit
        const changesToUndo: RuleModelChanges[] = [];
        while (op != null && op !== TRANSACTION_BOUNDARY) {
            changesToUndo.push(op);
            op = changeStack.pop();
        }
        return changesToUndo;
    };

    /**
     * Execute functions for model changes.
     */

    const addNode = (node: RuleEditorNode) => {
        setElements((elements) => [...elements, node]);
    };

    const addRedoRuleModelChange = (ruleModelChange: RuleModelChanges) => {
        ruleRedoStack.push(ruleModelChange);
        setCanRedo(true);
    };

    const addRuleModelChange = (ruleModelChange: RuleModelChanges) => {
        // Clear redo stack, starting a new "branch", former redo operations might have become invalid
        ruleRedoStack.splice(0);
        ruleUndoStack.push(ruleModelChange);
        setCanRedo(false);
        setCanUndo(true);
    };

    /** Executes a rule mode change operation on the rule model. */
    const executeRuleModelChange = (ruleModelChange: RuleModelChanges): void => {
        // TODO: Execute changes
    };

    /** Functions that change the model state. */ // TODO: Add undo/redo to these functions
    // Delete multiple elements and return updated elements array
    const deleteElements = (elementsToRemove, els: Array<FlowElement>) => {
        return removeElements(elementsToRemove, els);
    };

    /** Delete multiple nodes, e.g. from a selection. */
    const deleteNodes = (nodeIds: string[]) => {
        const nodeIdSet = new Set(nodeIds);
        setElements((els) => {
            const nodes = els.filter((n) => ruleEditorUtils.isNode(n) && nodeIdSet.has(n.id));
            if (nodes.length > 0) {
                return deleteElements(nodes, els);
            } else {
                return els;
            }
        });
    };

    /** Deletes a single rule node. */
    const deleteNode = (nodeId: string) => {
        setElements((els) => {
            const node = ruleEditorUtils.asNode(els.find((n) => ruleEditorUtils.isNode(n) && n.id === nodeId));
            if (node) {
                addRuleModelChange({
                    operations: [
                        {
                            type: "Delete node",
                            node,
                        },
                    ],
                });
                return deleteElements([node], els);
            } else {
                return els;
            }
        });
    };

    const executeModelEditOperation = (modelEditOperation: IRuleModelEditAction) => {
        switch (modelEditOperation.type) {
            case "transaction start":
                return startChangeTransaction();
            case "delete node":
                return deleteNode((modelEditOperation as DeleteNodeAction).nodeId);
            default:
                console.error("Tried to execute unsupported operation: " + modelEditOperation.type);
        }
    };

    const saveRule = () => {
        // TODO: Convert react-flow nodes to IRuleOperatorNodes
        return ruleEditorContext.saveRule([]);
    };

    /** Convert initial operator nodes to react-flow model. */
    React.useEffect(() => {
        if (
            ruleEditorContext.initialRuleOperatorNodes &&
            ruleEditorContext.operatorList &&
            elements.length === 0 &&
            reactFlowInstance
        ) {
            const operatorsNodes = ruleEditorContext.initialRuleOperatorNodes;
            // Create nodes
            const nodes = operatorsNodes.map((operatorNode) => {
                return ruleEditorUtils.createOperatorNode(operatorNode, reactFlowInstance, deleteNode, t);
            });
            // Create edges
            const edges: Edge[] = [];
            operatorsNodes.forEach((node) => {
                node.inputs.forEach((inputNodeId, idx) => {
                    if (inputNodeId) {
                        // Edge IDs do not currently matter
                        edges.push(ruleEditorUtils.createEdge(inputNodeId, node.nodeId, `${idx}`));
                    }
                });
            });
            setElements([...nodes, ...edges]);
            setRuleUndoStack([]);
            setRuleRedoStack([]);
        }
    }, [ruleEditorContext.initialRuleOperatorNodes, ruleEditorContext.operatorList, elements, reactFlowInstance]);

    return (
        <RuleEditorModelContext.Provider
            value={{
                elements,
                isReadOnly,
                setIsReadOnly,
                setReactFlowInstance,
                saveRule,
                executeModelEditOperation,
                undo,
                canUndo,
                redo,
                canRedo,
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
