import React from "react";
import { Edge, Elements, FlowElement, OnLoadParams, removeElements, useStoreActions } from "react-flow-renderer";
import { RuleEditorModelContext } from "./contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "./contexts/RuleEditorContext";
import utils from "./RuleEditor.utils";
import { useTranslation } from "react-i18next";
import { IRuleOperator, IRuleOperatorNode } from "./RuleEditor.typings";
import {
    AddEdge,
    AddNode,
    ChangeNodePosition,
    DeleteEdge,
    DeleteNode,
    RuleEditorNode,
    RuleModelChanges,
    RuleModelChangesFactory,
    RuleModelChangeType,
} from "./RuleEditorModel.typings";
import { XYPosition } from "react-flow-renderer/dist/types";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

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
    const updateNodePos = useStoreActions((actions) => actions.updateNodePos);

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

    /** Undo the last change-transaction. */
    const undo = (): boolean => {
        const changesToUndo = changeTransactionOperations(ruleUndoStack);
        if (changesToUndo.length > 0) {
            // Undo changes and create redo transaction
            startRedoChangeTransaction();
            // Changes must be redone in the reversed order
            const redoChanges = [...changesToUndo].reverse();
            redoChanges.forEach((change) => addRedoRuleModelChange(invertModelChanges(change)));
            // Execute it on the react-flow model
            setElements((els) => {
                let currentElements = els;
                changesToUndo.forEach((change) => {
                    currentElements = executeRuleModelChangeInternal(change, currentElements);
                });
                return currentElements;
            });
        }
        if (ruleUndoStack.length === 0) {
            setCanUndo(false);
        }
        return changesToUndo.length > 0;
    };

    /** Redo the last change-transaction. */
    const redo = () => {
        const changesToRedo = changeTransactionOperations(ruleRedoStack);
        if (changesToRedo.length > 0) {
            // Redo changes and create Undo transaction
            startChangeTransaction();
            // Changes must be redone in the reversed order
            const undoChanges = [...changesToRedo].reverse();
            undoChanges.forEach((change) => addRuleModelChange(invertModelChanges(change)));
            // Execute it on the react-flow model
            setElements((els) => {
                let currentElements = els;
                changesToRedo.forEach((change) => {
                    currentElements = executeRuleModelChangeInternal(change, currentElements);
                });
                return currentElements;
            });
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

    /** Adds a change action to the REDO stack. */
    const addRedoRuleModelChange = (ruleModelChange: RuleModelChanges) => {
        ruleRedoStack.push(ruleModelChange);
        setCanRedo(true);
    };

    /** Adds a change action to the history / UNDO stack. */
    const addRuleModelChange = (ruleModelChange: RuleModelChanges) => {
        // Clear redo stack, starting a new "branch", former redo operations might have become invalid
        ruleRedoStack.splice(0);
        ruleUndoStack.push(ruleModelChange);
        setCanRedo(false);
        setCanUndo(true);
    };

    /** Groups rule model changes, so they can be executed more efficiently. */
    const groupedRuleModelChanges = (ruleModelChange: RuleModelChanges): RuleModelChangeType[][] => {
        const groupedChanges: RuleModelChangeType[][] = [];
        let currentType: string | undefined = undefined;
        let currentChanges: RuleModelChangeType[] = [];
        // Add changes if there are some
        const addChanges = () => {
            if (currentChanges.length > 0) {
                groupedChanges.push(currentChanges);
                currentChanges = [];
            }
        };
        ruleModelChange.operations.forEach((op) => {
            if (op.type !== currentType) {
                addChanges();
                currentType = op.type;
            }
            currentChanges.push(op);
        });
        addChanges();
        return groupedChanges;
    };

    /**
     * Execute functions for model changes.
     */

    /** Executes a rule mode change operation on the rule model. */
    const executeRuleModelChangeInternal = (ruleModelChange: RuleModelChanges, currentElements: Elements): Elements => {
        const groupedChanges = groupedRuleModelChanges(ruleModelChange);
        let changedElements = currentElements;
        groupedChanges.forEach((groupedChange) => {
            switch (groupedChange[0].type) {
                case "Add node":
                    changedElements = addElementsInternal(
                        groupedChange.map((change) => (change as AddNode).node),
                        changedElements
                    );
                    break;
                case "Add edge":
                    changedElements = addElementsInternal(
                        groupedChange.map((change) => (change as AddEdge).edge),
                        changedElements
                    );
                    break;
                case "Delete node":
                    changedElements = deleteElementsInternal(
                        groupedChange.map((change) => (change as DeleteNode).node),
                        changedElements
                    );
                    break;
                case "Delete edge":
                    changedElements = deleteElementsInternal(
                        groupedChange.map((change) => (change as DeleteEdge).edge),
                        changedElements
                    );
                    break;
                case "Change node position":
                    changedElements = changeNodePositionsInternal(
                        new Map(
                            groupedChange.map((change) => {
                                const nodeChange = change as ChangeNodePosition;
                                return [nodeChange.nodeId, nodeChange.to];
                            })
                        ),
                        changedElements
                    );
                    break;
                case "Change node parameter":
                    // TODO
                    break;
            }
        });
        return changedElements;
    };

    /** Adds a rule model change action to the undo stack and executes the change on the model. */
    const addAndExecuteRuleModelChangeInternal = (
        ruleModelChange: RuleModelChanges,
        currentElements: Elements
    ): Elements => {
        addRuleModelChange(ruleModelChange);
        return executeRuleModelChangeInternal(ruleModelChange, currentElements);
    };

    /**
     * Functions that change the react-flow model state.
     **/
    // Add new elements to react-flow model
    const addElementsInternal = (elementsToAdd: FlowElement[], els: FlowElement[]): Elements => {
        return [...els, ...elementsToAdd];
    };
    // Delete multiple elements and return updated elements array
    const deleteElementsInternal = (elementsToRemove: FlowElement[], els: FlowElement[]): Elements => {
        return removeElements(elementsToRemove, els);
    };
    // Change the position of nodes
    const changeNodePositionsInternal = (nodesToMove: Map<string, XYPosition>, els: FlowElement[]): Elements => {
        return els.map((elem) => {
            if (utils.isNode(elem) && nodesToMove.has(elem.id)) {
                const node = utils.asNode(elem)!!;
                const newPosition = nodesToMove.get(node.id)!!;
                const movedNode: RuleEditorNode = {
                    ...node,
                    data: node.data ? { ...node.data } : undefined,
                    position: newPosition,
                };
                // TODO: Check if the following line is really needed
                setTimeout(() => updateNodePos({ id: node.id, pos: newPosition }), 50);
                return movedNode;
            } else {
                return elem;
            }
        });
    };
    /**
     * Public interface model change functions.
     *
     * All public change functions must call addAndExecuteRuleModelChangeInternal in order to register and execute the changes.
     * The must not change model elements directly.
     **/

    /** Add a new node from the rule operators list. */
    const addNode = (ruleOperator: IRuleOperator, position: XYPosition) => {
        if (reactFlowInstance) {
            const ruleNode = ruleEditorContext.convertRuleOperatorToRuleNode(ruleOperator);
            ruleNode.position = position;
            const newNode = utils.createNewOperatorNode(ruleNode, reactFlowInstance, deleteNode, t, elements);
            setElements((els) => {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addNode(newNode), els);
            });
        }
    };

    /** Deletes a single rule node. */
    const deleteNode = (nodeId: string) => {
        setElements((els) => {
            const node = utils.asNode(els.find((n) => utils.isNode(n) && n.id === nodeId));
            if (node) {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteNode(node), els);
            } else {
                return els;
            }
        });
    };

    /** Delete multiple nodes, e.g. from a selection. */
    const deleteNodes = (nodeIds: string[]) => {
        setElements((els) => {
            const nodes = utils.nodesById(els, nodeIds);
            if (nodes.length > 0) {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteNodes(nodes), els);
            } else {
                return els;
            }
        });
    };

    /** Copy and paste nodes with a given offset. */
    const copyAndPasteNodes = (nodeIds: string[], offset: XYPosition) => {
        setElements((els) => {
            const nodes = utils.nodesById(els, nodeIds);
            const changePositionActions: ChangeNodePosition[] = nodes.map((node) => ({
                type: "Change node position",
                nodeId: node.id,
                from: node.position,
                to: {
                    x: node.position.x + offset.x,
                    y: node.position.y + offset.y,
                },
            }));
            return addAndExecuteRuleModelChangeInternal({ operations: changePositionActions }, els);
        });
    };

    /** Move a node to a new position. */
    const moveNode = (nodeId: string, newPosition: XYPosition) => {
        setElements((els) => {
            const node = utils.nodeById(els, nodeId);
            if (node) {
                return addAndExecuteRuleModelChangeInternal(
                    RuleModelChangesFactory.changeNodePosition(nodeId, node.position, newPosition),
                    els
                );
            } else {
                return els;
            }
        });
    };

    /** Save the current rule. */
    const saveRule = () => {
        const nodes: RuleEditorNode[] = [];
        const nodeInputEdges: Map<string, Edge[]> = new Map();
        const inputEdgesByNodeId = (nodeId: string): Edge[] => {
            if (nodeInputEdges.has(nodeId)) {
                return nodeInputEdges.get(nodeId)!!;
            } else {
                const newArray = [];
                nodeInputEdges.set(nodeId, newArray);
                return newArray;
            }
        };
        elements.forEach((elem) => {
            if (utils.isNode(elem)) {
                nodes.push(utils.asNode(elem)!!);
            } else {
                const edge = utils.asEdge(elem)!!;
                inputEdgesByNodeId(edge.target).push(edge);
            }
        });
        const ruleOperatorNodes = nodes.map((node) => {
            const inputHandleIds = utils.inputHandles(node).map((h) => h.id);
            const inputEdges = nodeInputEdges.get(node.id) ?? [];
            const inputEdgeMap = new Map(inputEdges.map((e) => [e.id, e.source]));
            const inputs = inputHandleIds.map((handleId) => (handleId ? inputEdgeMap.get(handleId) : undefined));
            const originalNode = node.data?.businessData.originalRuleOperatorNode!!;
            const ruleOperatorNode: IRuleOperatorNode = {
                inputs,
                label: originalNode.label, // TODO: Can the label change?
                nodeId: node.id,
                parameters: {}, // TODO: Handle parameters
                pluginId: originalNode.pluginId,
                pluginType: originalNode.pluginType,
                portSpecification: originalNode.portSpecification,
            };
            return ruleOperatorNode;
        });
        return ruleEditorContext.saveRule(ruleOperatorNodes);
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
                return utils.createOperatorNode(operatorNode, reactFlowInstance, deleteNode, t);
            });
            // Create edges
            const edges: Edge[] = [];
            operatorsNodes.forEach((node) => {
                node.inputs.forEach((inputNodeId, idx) => {
                    if (inputNodeId) {
                        // Edge IDs do not currently matter
                        edges.push(utils.createEdge(inputNodeId, node.nodeId, `${idx}`));
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
                undo,
                canUndo,
                redo,
                canRedo,
                executeModelEditOperation: {
                    startChangeTransaction,
                    addNode,
                    deleteNode,
                    deleteNodes,
                    copyAndPasteNodes,
                    moveNode,
                },
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
