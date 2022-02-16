import React, { useState } from "react";
import { Edge, Elements, OnLoadParams, removeElements, useStoreActions, useStoreState } from "react-flow-renderer";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "../contexts/RuleEditorContext";
import { IOperatorCreateContext, IOperatorNodeOperations, ruleEditorModelUtilsFactory } from "./RuleEditorModel.utils";
import { useTranslation } from "react-i18next";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    RuleOperatorNodeParameters
} from "../RuleEditor.typings";
import {
    AddEdge,
    AddNode,
    ChangeNodeParameter,
    ChangeNodePosition,
    DeleteEdge,
    DeleteNode,
    RuleEditorNode,
    RuleModelChanges,
    RuleModelChangesFactory,
    RuleModelChangeType,
} from "./RuleEditorModel.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import { NodeContent } from "../view/ruleNode/NodeContent";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

// Object to denote transaction boundaries between change operations
const TRANSACTION_BOUNDARY = "Transaction boundary";
type ChangeStackType = RuleModelChanges | "Transaction boundary";

/** The actual rule model, i.e. the model that is displayed in the editor.
 *  All rule model changes must happen here. */
export const RuleEditorModel = ({ children }: RuleEditorModelProps) => {
    const { t } = useTranslation();
    /** If set, then the model cannot be modified. */
    const [isReadOnly, setIsReadOnly] = React.useState<boolean>(false);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    /** The nodes and edges of the rule editor. */
    const [elements, setElements] = React.useState<Elements>([]);
    /** Rule editor context. */
    const ruleEditorContext = React.useContext<RuleEditorContextProps>(RuleEditorContext);
    /** The rule editor change history that will be used to UNDO changes. The changes are in the order they have been executed. */
    const [ruleUndoStack] = React.useState<ChangeStackType[]>([]);
    /** If there are changes that can be undone. */
    const [canUndo, setCanUndo] = React.useState<boolean>(false);
    /** Stores the REDO history. The order is how the changes were executed by the corresponding UNDO operation. */
    const [ruleRedoStack] = React.useState<ChangeStackType[]>([]);
    /** If there are changes that can be redone, i.e. that have been previously undone. */
    const [canRedo, setCanRedo] = React.useState<boolean>(false);
    const [utils] = React.useState(ruleEditorModelUtilsFactory());
    /** react-flow function to update a node position in the canvas. Just changing the position of the elements is not enough. */
    const updateNodePos = useStoreActions((actions) => actions.updateNodePos);
    /** The current zoom factor. */
    const [, , zoom] = useStoreState((state) => state.transform);
    /** If post-initializations have been executed. */
    const [postInit, setPostInit] = useState(false);
    /** Manages the parameters of rule nodes. This is done for performance reasons. Only stores diffs to the original value. */
    const [nodeParameters] = React.useState<Map<string, Map<string, string | undefined>>>(new Map());

    /** Convert initial operator nodes to react-flow model. */
    React.useEffect(() => {
        if (
            ruleEditorContext.initialRuleOperatorNodes &&
            ruleEditorContext.operatorSpec &&
            ruleEditorContext.operatorList &&
            elements.length === 0 &&
            reactFlowInstance
        ) {
            initModel();
        }
    }, [
        ruleEditorContext.initialRuleOperatorNodes,
        ruleEditorContext.operatorList,
        elements.length,
        reactFlowInstance,
    ]);

    React.useEffect(() => {
        if (elements.length > 0 && !postInit) {
            setTimeout(() => {
                reactFlowInstance?.fitView();
                reactFlowInstance?.zoomTo(0.75);
            }, 1);
            setPostInit(true);
        }
    }, [ruleEditorContext.editedItem, postInit, elements]);

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
            // Changes must be undone in the reversed order
            const reversedChanges = [...changesToUndo].reverse().map((change) => invertModelChanges(change));
            reversedChanges.forEach((change) => addRedoRuleModelChange(change));
            // Execute it on the react-flow model
            setElements((els) => {
                let currentElements = els;
                reversedChanges.forEach((change) => {
                    currentElements = executeRuleModelChangeInternal(change, currentElements, true);
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
            const invertedChanges = [...changesToRedo].reverse().map((change) => invertModelChanges(change));
            invertedChanges.forEach((change) => addRuleModelChange(change, false));
            // Execute it on the react-flow model
            setElements((els) => {
                let currentElements = els;
                invertedChanges.forEach((change) => {
                    currentElements = executeRuleModelChangeInternal(change, currentElements, true);
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
        return changesToUndo.reverse();
    };

    /** Adds a change action to the REDO stack. */
    const addRedoRuleModelChange = (ruleModelChange: RuleModelChanges) => {
        ruleRedoStack.push(ruleModelChange);
        setCanRedo(true);
    };

    /** Adds a change action to the history / UNDO stack. */
    const addRuleModelChange = (ruleModelChange: RuleModelChanges, resetRedoStack: boolean) => {
        // Clear redo stack, starting a new "branch", former redo operations might have become invalid
        addOrMergeRuleModelChange(ruleModelChange);
        if (resetRedoStack) {
            ruleRedoStack.splice(0);
            setCanRedo(false);
        }
        setCanUndo(true);
    };

    /** Adds a rule model change and in some cases merges them. */
    const addOrMergeRuleModelChange = (ruleModelChanges: RuleModelChanges) => {
        const lastChange = asChangeNodeParameter(ruleUndoStack[ruleUndoStack.length - 1]);
        const parameterChange = asChangeNodeParameter(ruleModelChanges);
        if (
            parameterChange &&
            lastChange &&
            parameterChange.nodeId === lastChange.nodeId &&
            parameterChange.parameterId === lastChange.parameterId
        ) {
            ruleUndoStack.pop();
            ruleUndoStack.push(ruleModelChanges);
        } else {
            ruleUndoStack.push(ruleModelChanges);
        }
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

    /** Executes a rule mode change operation on the rule model.
     *
     * @param ruleModelChange The operations that are executed on the rule model.
     * @param currentElements The current rule tree.
     * @param isUndoOrRedo    If this is triggered by an undo/redo action. If true then some post-processing might be needed in some cases.
     */
    const executeRuleModelChangeInternal = (
        ruleModelChange: RuleModelChanges,
        currentElements: Elements,
        isUndoOrRedo: boolean = false
    ): Elements => {
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
                    const nodesParameterDiff: Map<string, Map<string, string | undefined>> = new Map();
                    groupedChange.forEach((change) => {
                        const nodeChange = change as ChangeNodeParameter;
                        const nodeParameterDiff =
                            nodesParameterDiff.get(nodeChange.nodeId) ?? new Map<string, string | undefined>();
                        nodeParameterDiff.set(nodeChange.parameterId, nodeChange.to);
                        nodesParameterDiff.set(nodeChange.nodeId, nodeParameterDiff);
                    });
                    // Does not change the actual elements
                    changeNodeParametersInternal(nodesParameterDiff);
                    if (isUndoOrRedo) {
                        changedElements = currentElements.map((elem) => {
                            if (utils.isNode(elem) && nodesParameterDiff.has(elem.id)) {
                                const currentRuleNode = utils.asNode(elem)!!;
                                const businessData = currentRuleNode.data.businessData;
                                const op = businessData.originalRuleOperatorNode;
                                return {
                                    ...currentRuleNode,
                                    data: {
                                        ...currentRuleNode.data,
                                        businessData: { ...businessData, updateSwitch: !businessData.updateSwitch },
                                        content: (
                                            <NodeContent
                                                nodeOperations={operatorNodeOperationsInternal}
                                                nodeId={currentRuleNode.id}
                                                tags={op.tags}
                                                operatorContext={operatorNodeCreateContextInternal(
                                                    op.pluginId,
                                                    reactFlowInstance!!,
                                                    ruleEditorContext.operatorSpec!!
                                                )}
                                                nodeParameters={{
                                                    ...op.parameters,
                                                    ...Object.fromEntries(nodeParameters.get(elem.id)!!.entries()),
                                                }}
                                                updateSwitch={!businessData.updateSwitch}
                                            />
                                        ),
                                    },
                                };
                            } else {
                                return elem;
                            }
                        });
                    }
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
        addRuleModelChange(ruleModelChange, true);
        return executeRuleModelChangeInternal(ruleModelChange, currentElements);
    };

    /**
     * Functions that change the react-flow model state.
     **/
    // Add new elements to react-flow model
    const addElementsInternal = (elementsToAdd: Elements, els: Elements): Elements => {
        return [...els, ...elementsToAdd];
    };
    // Delete multiple elements and return updated elements array
    const deleteElementsInternal = (elementsToRemove: Elements, els: Elements): Elements => {
        return removeElements(elementsToRemove, els);
    };
    // Change the position of nodes
    const changeNodePositionsInternal = (nodesToMove: Map<string, XYPosition>, els: Elements): Elements => {
        return els.map((elem) => {
            if (utils.isNode(elem) && nodesToMove.has(elem.id)) {
                const node = utils.asNode(elem)!!;
                const newPosition = nodesToMove.get(node.id)!!;
                const movedNode: RuleEditorNode = {
                    ...node,
                    data: { ...node.data },
                    position: newPosition,
                };
                // This must be triggered, else the new position will not be rendered
                setTimeout(() => updateNodePos({ id: node.id, pos: newPosition }), 50);
                return movedNode;
            } else {
                return elem;
            }
        });
    };
    // Changes the node parameters
    const changeNodeParametersInternal = (
        nodeParametersToChange: Map<string, Map<string, string | undefined>>
    ): void => {
        nodeParametersToChange.forEach((parameterChanges, nodeId) => {
            const parameterDiff = nodeParameters.get(nodeId) ?? new Map();
            nodeParameters.set(nodeId, new Map([...parameterDiff, ...parameterChanges]));
        });
    };

    /** Should be called every time a node is created. */
    const initNodeParametersInternal = (nodeId: string, parameters: RuleOperatorNodeParameters) => {
        const parameterMap = new Map(Object.entries(parameters));
        nodeParameters.set(nodeId, parameterMap);
    }

    /**
     * Public interface model change functions.
     *
     * All public change functions must call addAndExecuteRuleModelChangeInternal in order to register and execute the changes.
     * The must not change model elements directly.
     **/

    /** Add a new node from the rule operators list. */
    const addNode = (ruleOperator: IRuleOperator, position: XYPosition) => {
        if (reactFlowInstance && ruleEditorContext.operatorSpec) {
            const ruleNode = ruleEditorContext.convertRuleOperatorToRuleNode(ruleOperator);
            ruleNode.position = position;
            setElements((els) => {
                const newNode = utils.createNewOperatorNode(
                    ruleNode,
                    operatorNodeOperationsInternal,
                    operatorNodeCreateContextInternal(
                        ruleOperator.pluginId,
                        reactFlowInstance,
                        ruleEditorContext.operatorSpec!!
                    )
                );
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addNode(newNode), els);
            });
        }
    };

    // Finds all edges that are connected to any of the given nodes.
    const findEdgesOfNodes = (nodeIds: Set<string>, elements: Elements): Edge[] => {
        return elements.filter((elem) => {
            const edge = utils.asEdge(elem);
            return edge && (nodeIds.has(edge.source) || nodeIds.has(edge.target));
        }) as Edge[];
    };

    /** Deletes a single rule node. */
    const deleteNode = (nodeId: string) => {
        setElements((els) => {
            const node = utils.asNode(els.find((n) => utils.isNode(n) && n.id === nodeId));
            if (node) {
                // Need to record edge changes first
                const edges = findEdgesOfNodes(new Set([node.id]), els);
                const withoutEdges = addAndExecuteRuleModelChangeInternal(
                    RuleModelChangesFactory.deleteEdges(edges),
                    els
                );
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteNode(node), withoutEdges);
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
                // Need to record edge changes first
                const edges = findEdgesOfNodes(new Set(nodeIds), els);
                const withoutEdges = addAndExecuteRuleModelChangeInternal(
                    RuleModelChangesFactory.deleteEdges(edges),
                    els
                );
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteNodes(nodes), withoutEdges);
            } else {
                return els;
            }
        });
    };

    const addEdge = (sourceNodeId: string, targetNodeId: string, targetHandleId: string) => {
        setElements((els) => {
            const edge = utils.createEdge(sourceNodeId, targetNodeId, targetHandleId);
            return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addEdge(edge), els);
        });
    };

    /** Deletes a single edge. */
    const deleteEdge = (edgeId: string) => {
        setElements((els) => {
            const edge = utils.edgeById(els, edgeId);
            if (edge) {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteEdge(edge), els);
            } else {
                return els;
            }
        });
    };

    /** Copy and paste nodes with a given offset. */
    const copyAndPasteNodes = (nodeIds: string[], offset: XYPosition) => {
        setElements((els) => {
            const nodes = utils.nodesById(els, nodeIds);
            const nodeIdMap = new Map<string, string>();
            const newNodes: RuleEditorNode[] = nodes.map((node) => {
                const newNodeId = utils.freshNodeId(
                    node.data?.businessData.originalRuleOperatorNode.pluginId ?? "node_id"
                );
                nodeIdMap.set(node.id, newNodeId);
                return {
                    ...node,
                    id: newNodeId,
                    data: { ...node.data },
                    position: { x: node.position.x + offset.x, y: node.position.y + offset.y },
                };
            });
            const newEdges: Edge[] = [];
            els.forEach((elem) => {
                if (utils.isEdge(elem)) {
                    const edge = utils.asEdge(elem)!!;
                    if (nodeIdMap.has(edge.source) && nodeIdMap.has(edge.target)) {
                        const newEdge = utils.createEdge(
                            nodeIdMap.get(edge.source)!!,
                            nodeIdMap.get(edge.target)!!,
                            edge.targetHandle!!
                        );
                        newEdges.push(newEdge);
                    }
                }
            });
            const withNodes = addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addNodes(newNodes), els);
            return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addEdges(newEdges), withNodes);
        });
    };

    /** Converts this rule change to a single 'change node parameter' action if possible, else returns undefined. */
    const asChangeNodeParameter = (ruleModelChanges: ChangeStackType | undefined): ChangeNodeParameter | undefined => {
        return typeof ruleModelChanges === "object" &&
            ruleModelChanges.operations.length === 1 &&
            ruleModelChanges.operations[0].type === "Change node parameter"
            ? (ruleModelChanges.operations[0] as ChangeNodeParameter)
            : undefined;
    };

    /** Return the last rule parameter change if no other actions was done after it and it affected only a single parameter. */
    const lastRuleParameterChange = (): ChangeNodeParameter | undefined => {
        const lastChange = ruleUndoStack[ruleUndoStack.length - 1];
        return asChangeNodeParameter(lastChange);
    };

    const lastChangeIsTransactionStart = () => {
        return ruleUndoStack[ruleUndoStack.length - 1] === TRANSACTION_BOUNDARY;
    };

    const currentParameterValue = (
        nodeId: string,
        parameterId: string
    ): string | undefined => {
        const nodeDiff = nodeParameters.get(nodeId);
        if (nodeDiff) {
            return nodeDiff.get(parameterId);
        } else {
            // Node parameters must be initialized at this point
            console.warn("No parameters for node " + nodeId + " exist!")
        }
    };

    /** Changes a single node parameter to a new value. */
    const changeNodeParameter = (
        nodeId: string,
        parameterId: string,
        newValue: string | undefined,
        autoStartTransaction: boolean = true
    ) => {
        const changeValue = (from: string | undefined, to: string | undefined) => {
            // This does not change the actual elements, so we provide dummy elements
            addAndExecuteRuleModelChangeInternal(
                RuleModelChangesFactory.changeNodeParameter(nodeId, parameterId, from, to),
                []
            );
        }
        // Merge parameter changes done to the same node/parameter subsequently, so it becomes a single undo operation
        const recentParameterChange = lastRuleParameterChange();
        const sameParameterChanged =
            recentParameterChange &&
            recentParameterChange.nodeId === nodeId &&
            recentParameterChange.parameterId === parameterId;
        if (sameParameterChanged) {
            ruleUndoStack.pop();
        }
        // Start a new transaction when previous action was a non-parameter change or another node/parameter was changed
        if (autoStartTransaction && !lastChangeIsTransactionStart() && !sameParameterChanged) {
            startChangeTransaction();
        }
        if (sameParameterChanged) {
            changeValue(recentParameterChange!!.from, newValue)
        } else {
            changeValue(currentParameterValue(nodeId, parameterId), newValue)
        }
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

    /** Layout the rule nodes, since this must be async it cannot return the new elements directly in the setElements function. */
    const autoLayoutInternal = async (elements: Elements, addChangeHistory: boolean): Promise<Elements> => {
        const newLayout = await utils.autoLayout(elements, zoom);
        const changeNodePositionOperations: ChangeNodePosition[] = [];
        utils.elementNodes(elements).forEach((node) => {
            const newPosition = newLayout.get(node.id);
            if (newPosition && (newPosition.x !== node.position.x || newPosition.y !== node.position.y)) {
                changeNodePositionOperations.push({
                    type: "Change node position",
                    nodeId: node.id,
                    from: node.position,
                    to: newPosition,
                });
            }
        });
        if (changeNodePositionOperations.length > 0) {
            startChangeTransaction();
            const changeNodePositions = { operations: changeNodePositionOperations };
            return addChangeHistory
                ? addAndExecuteRuleModelChangeInternal(changeNodePositions, elements)
                : executeRuleModelChangeInternal(changeNodePositions, elements);
        } else {
            return elements;
        }
    };

    const operatorNodeOperationsInternal: IOperatorNodeOperations = {
        handleDeleteNode: deleteNode,
        handleParameterChange: changeNodeParameter,
    };

    // Context for creating new nodes
    const operatorNodeCreateContextInternal = (
        operatorPluginId: string,
        reactFlowInstance: OnLoadParams,
        operatorSpec: Map<string, Map<string, IParameterSpecification>>
    ): IOperatorCreateContext => ({
        operatorParameterSpecification: operatorSpec.get(operatorPluginId) ?? new Map(),
        t,
        reactFlowInstance,
        currentValue: currentParameterValue,
        initParameters: initNodeParametersInternal
    });

    /** Auto-layout the rule nodes.
     *
     * @param noHistory If the change should be tracked or not.
     */
    const autoLayout = (noHistory: boolean = false) => {
        setElements((elements) => {
            autoLayoutInternal(elements, noHistory);
            return elements;
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
            const inputHandleIds = utils.inputHandles(node).map((h) => h.id!!);
            const inputEdges = nodeInputEdges.get(node.id) ?? [];
            const inputEdgeMap = new Map(inputEdges.map((e) => [e.targetHandle, e.source]));
            const inputs = inputHandleIds.map((handleId) => inputEdgeMap.get(handleId));
            const portSpec = node.data.businessData.originalRuleOperatorNode.portSpecification;
            // Remove undefined input ports above last defined input if spec allows it
            if (!portSpec.maxInputPorts) {
                while (inputs.length > portSpec.minInputPorts - 1 && inputs[inputs.length - 1] == null) {
                    inputs.pop();
                }
            }
            const originalNode = node.data?.businessData.originalRuleOperatorNode!!;
            const parameterDiff = nodeParameters.get(node.id);
            const ruleOperatorNode: IRuleOperatorNode = {
                inputs,
                label: originalNode.label,
                nodeId: node.id,
                parameters: parameterDiff
                    ? Object.fromEntries(
                          Object.entries(originalNode.parameters).map(([parameterId, parameterValue]) => {
                              return [
                                  parameterId,
                                  parameterDiff && parameterDiff.has(parameterId)
                                      ? parameterDiff.get(parameterId)
                                      : parameterValue,
                              ];
                          })
                      )
                    : originalNode.parameters,
                pluginId: originalNode.pluginId,
                pluginType: originalNode.pluginType,
                portSpecification: originalNode.portSpecification,
                position: node.position,
            };
            return ruleOperatorNode;
        });
        return ruleEditorContext.saveRule(ruleOperatorNodes);
    };

    const initModel = async () => {
        const handleDeleteNode = (nodeId: string) => {
            startChangeTransaction();
            deleteNode(nodeId);
        };
        nodeParameters.clear();
        const operatorsNodes = ruleEditorContext.initialRuleOperatorNodes;
        // Create nodes
        let needsLayout = false;
        const nodes = operatorsNodes!!.map((operatorNode) => {
            needsLayout = needsLayout || !operatorNode.position;
            return utils.createOperatorNode(
                operatorNode,
                { ...operatorNodeOperationsInternal, handleDeleteNode },
                operatorNodeCreateContextInternal(
                    operatorNode.pluginId,
                    reactFlowInstance!!,
                    ruleEditorContext.operatorSpec!!
                )
            );
        });
        // Create edges
        const edges: Edge[] = [];
        operatorsNodes!!.forEach((node) => {
            node.inputs.forEach((inputNodeId, idx) => {
                if (inputNodeId) {
                    // Edge IDs do not currently matter
                    edges.push(utils.createEdge(inputNodeId, node.nodeId, `${idx}`));
                }
            });
        });
        let elems: Elements = [...nodes, ...edges];
        if (needsLayout) {
            elems = await autoLayoutInternal(elems, false);
        }
        setElements(elems);
        utils.initNodeBaseIds(nodes);
        ruleUndoStack.splice(0);
        ruleRedoStack.splice(0);
        setPostInit(false);
    };

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
                    changeNodeParameter,
                    addEdge,
                    deleteEdge,
                    autoLayout,
                },
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
