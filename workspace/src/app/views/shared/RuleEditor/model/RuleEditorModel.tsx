import React, { useEffect } from "react";
import {
    Edge,
    Elements,
    OnLoadParams,
    removeElements,
    useStoreActions,
    useStoreState,
    useUpdateNodeInternals,
    useZoomPanHelper,
} from "react-flow-renderer";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { RuleEditorContext, RuleEditorContextProps } from "../contexts/RuleEditorContext";
import { IOperatorCreateContext, IOperatorNodeOperations, ruleEditorModelUtilsFactory } from "./RuleEditorModel.utils";
import { useTranslation } from "react-i18next";
import {
    IParameterSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    RuleEditorValidationNode,
    RuleOperatorNodeParameters,
} from "../RuleEditor.typings";
import {
    AddEdge,
    AddNode,
    ChangeNodeParameter,
    ChangeNodePosition,
    ChangeNumberOfInputHandles,
    DeleteEdge,
    DeleteNode,
    RuleEditorNode,
    RuleEditorNodeParameterValue,
    RuleModelChanges,
    RuleModelChangesFactory,
    RuleModelChangeType,
} from "./RuleEditorModel.typings";
import { Connection, XYPosition } from "react-flow-renderer/dist/types";
import { NodeContent, RuleNodeContentProps } from "../view/ruleNode/NodeContent";
import { maxNumberValuePicker, setConditionalMap } from "../../../../utils/basicUtils";
import { HighlightingState } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeContent";
import { RuleEditorEvaluationContext, RuleEditorEvaluationContextProps } from "../contexts/RuleEditorEvaluationContext";

export interface RuleEditorModelProps {
    /** The children that work on this rule model. */
    children: JSX.Element | JSX.Element[];
}

// Object to denote transaction boundaries between change operations
const TRANSACTION_BOUNDARY = "Transaction boundary";
type ChangeStackType = RuleModelChanges | "Transaction boundary";

/** Used for internal use. Allows to traverse the rule tree efficiently. */
interface RuleTreeNode {
    inputs: (string | undefined)[];
    output: string | undefined;
    node: IRuleOperatorNode;
}

/** The actual rule model, i.e. the model that is displayed in the editor.
 *  All rule model changes must happen here.
 *  It contains the main (core) rule editor logic. */
export const RuleEditorModel = ({ children }: RuleEditorModelProps) => {
    const { t } = useTranslation();
    /** If set, then the model cannot be modified. */
    const [readOnlyState] = React.useState<{ enabled: boolean }>({ enabled: false });
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    /** The nodes and edges of the rule editor. */
    const [elements, setElements] = React.useState<Elements>([]);
    /** Track the current elements, since the API methods changing the elements when run subsequently will otherwise work with the same elements.
     * Use the function changeElementsInternal to modify the elements instead of directly changing them. */
    const [current] = React.useState<{ elements: Elements; evaluateQuickly: boolean }>({
        elements: [],
        evaluateQuickly: false,
    });
    current.elements = elements;
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
    /** react-flow function to update a node position in the canvas. Just changing the position of the elements is not enough. */
    const updateNodePos = useStoreActions((actions) => actions.updateNodePos);
    /** The current zoom factor. */
    const [, , zoom] = useStoreState((state) => state.transform);
    /** Manages the parameters of rule nodes. This is done for performance reasons. Only stores diffs to the original value. */
    const [nodeParameters] = React.useState<Map<string, Map<string, RuleEditorNodeParameterValue>>>(new Map());
    /** Update the internals of a node. This needs to be called in some cases in order to update the appearance of a node. */
    const updateNodeInternals = useUpdateNodeInternals();
    const resetSelectedElements = useStoreActions((a) => a.resetSelectedElements);
    const setSelectedElements = useStoreActions((a) => a.setSelectedElements);
    const unsetUserSelection = useStoreActions((actions) => actions.unsetUserSelection);
    const setInteractive = useStoreActions((a) => a.setInteractive);
    /** Map from node ID to (original) rule operator node. Used for validating connections. */
    const [nodeMap] = React.useState<Map<string, RuleTreeNode>>(new Map());
    const [evaluationCounter, setEvaluationCounter] = React.useState(0);
    const ruleEvaluationContext: RuleEditorEvaluationContextProps =
        React.useContext<RuleEditorEvaluationContextProps>(RuleEditorEvaluationContext);
    const [evaluateQuickly, setEvaluateQuickly] = React.useState(false);
    const [readOnly, _setIsReadOnly] = React.useState(false);
    const [utils] = React.useState(ruleEditorModelUtilsFactory(() => (nodeMap ? "edge" : "default")));
    /** react-flow related functions */
    const { setCenter } = useZoomPanHelper();

    const edgeType = (ruleOperatorNode?: IRuleOperatorNode) => {
        if (ruleOperatorNode) {
            switch (ruleOperatorNode.pluginType) {
                case "PathInputOperator":
                case "TransformOperator":
                    return "value";
                case "ComparisonOperator":
                case "AggregationOperator":
                    return "score";
                default:
                    return "step";
            }
        }
        return "step";
    };

    const setIsReadOnly = (enabled: boolean) => {
        readOnlyState.enabled = enabled;
        _setIsReadOnly(enabled);
    };

    React.useEffect(() => {
        // Reset model on ID changes
        setElements([]);
    }, [ruleEditorContext.projectId, ruleEditorContext.editedItemId]);

    /** Convert initial operator nodes to react-flow model. */
    React.useEffect(() => {
        if (
            ruleEditorContext.initialRuleOperatorNodes &&
            ruleEditorContext.operatorSpec &&
            ruleEditorContext.operatorList &&
            ruleEditorContext.editedItem &&
            reactFlowInstance
        ) {
            initModel();
        }
    }, [
        ruleEditorContext.projectId,
        ruleEditorContext.initialRuleOperatorNodes,
        ruleEditorContext.operatorSpec,
        ruleEditorContext.operatorList,
        ruleEditorContext.editedItem,
        reactFlowInstance,
    ]);

    React.useEffect(() => {
        if (ruleEditorContext.readOnlyMode) {
            setIsReadOnly(true);
            setInteractive(false);
        }
    }, [ruleEditorContext.readOnlyMode]);

    /** Validates a connection before its creation. */
    const isValidConnection = (connection: Connection) => {
        if (connection.source && connection.target && connection.targetHandle) {
            return isValidEdge(connection.source, connection.target, connection.targetHandle);
        } else {
            return true;
        }
    };

    const isValidEdge = (sourceNodeId: string, targetNodeId: string, targetHandleId: string) => {
        // Convert rule tree nodes to a real (lazy) tree structure for validation convenience
        const convertNode = (ruleTreeNode: RuleTreeNode): RuleEditorValidationNode => {
            return {
                node: ruleTreeNode.node,
                inputs: () => {
                    return ruleTreeNode.inputs.map((input) => {
                        return input && nodeMap.has(input) ? convertNode(nodeMap.get(input)!!) : undefined;
                    });
                },
                output: () => {
                    return ruleTreeNode.output && nodeMap.has(ruleTreeNode.output)
                        ? convertNode(nodeMap.get(ruleTreeNode.output)!!)
                        : undefined;
                },
            };
        };
        if (sourceNodeId === targetNodeId) {
            return false;
        }
        const sourceNode = nodeMap.get(sourceNodeId);
        const targetNode = nodeMap.get(targetNodeId);
        const targetInputIdx = Number.parseInt(targetHandleId);
        return sourceNode && targetNode && Number.isInteger(targetInputIdx)
            ? ruleEditorContext.validateConnection(convertNode(sourceNode), convertNode(targetNode), targetInputIdx)
            : true;
    };

    // Start
    useEffect(() => {
        if (evaluateQuickly) {
            const timeout = setTimeout(
                () => ruleEvaluationContext.startEvaluation(ruleOperatorNodes(), ruleEditorContext.editedItem, true),
                500
            );
            return () => clearTimeout(timeout);
        }
    }, [evaluationCounter, evaluateQuickly]);

    /** Inline rule evaluation */
    React.useEffect(() => {
        current.evaluateQuickly =
            ruleEvaluationContext.supportsEvaluation &&
            ruleEvaluationContext.supportsQuickEvaluation &&
            ruleEvaluationContext.evaluationResultsShown;
        setEvaluateQuickly(current.evaluateQuickly);
    }, [
        ruleEvaluationContext.supportsEvaluation,
        ruleEvaluationContext.supportsQuickEvaluation,
        ruleEvaluationContext.evaluationResultsShown,
    ]);

    React.useEffect(() => {
        const error = ruleEvaluationContext.ruleValidationError;
        if (error && error.nodeErrors.length > 0) {
            const nodeId = error.nodeErrors[0].nodeId;
            const node = utils.nodeById(elements, nodeId);
            if (node) {
                centerNodeInCanvas(node);
                highlightNodes([nodeId], "warning", true);
            }
        } else {
            clearHighlighting();
        }
    }, [ruleEvaluationContext.ruleValidationError]);

    // Sets the quick evaluation flag to signal that a new quick evaluation should be triggered.
    const triggerQuickEvaluation = () => {
        // Only trigger if it currently makes sense
        if (current.evaluateQuickly) {
            setEvaluationCounter((c) => c + 1);
        }
    };

    /** Elements should only be changed via this function when needing the current elements.
     *
     * @param changeFn      The function that changes the elements.
     * @param fixNodeInputs Either false for not doing anything or an array of node IDs that should be checked if they need
     *                      a change of input handles.
     */
    const changeElementsInternal = (changeFn: (elements: Elements) => Elements, fixNodeInputs: boolean = false) => {
        current.elements = changeFn(current.elements);
        if (fixNodeInputs) {
            current.elements = fixNodeInputPortsInternal(current.elements);
        }
        setElements(current.elements);
    };

    /** Checks if there is the right amount of input handles per node and adjusts them if needed. */
    const fixNodeInputPortsInternal = (elements: Elements): Elements => {
        const maxOccupiedHandle = new Map<string, number>();
        // Compute the larges handle index per node
        utils.elementEdges(elements).forEach((edge) => {
            const targetHandle = Number.parseInt(edge.targetHandle ?? "");
            if (!Number.isNaN(targetHandle)) {
                setConditionalMap(maxOccupiedHandle, edge.target, targetHandle, maxNumberValuePicker);
            }
        });
        const handleChangeOperations: ChangeNumberOfInputHandles[] = [];
        const changedElements = elements.map((elem) => {
            if (utils.isNode(elem)) {
                const node = utils.asNode(elem)!!;
                const dynamicPorts = node.data.businessData.dynamicPorts ?? false;
                const currentInputHandles = utils.inputHandles(node);
                const maxOccupiedHandleId = maxOccupiedHandle.get(node.id) ?? -1;
                // Only need to adjust nodes with dynamic input ports that have too many or not enough handles
                if (
                    dynamicPorts &&
                    (currentInputHandles.length > maxOccupiedHandleId + 2 ||
                        currentInputHandles.length <= maxOccupiedHandleId + 1)
                ) {
                    const newNumberOfHandles = maxOccupiedHandleId + 2;
                    handleChangeOperations.push({
                        type: "Change number of input handles",
                        nodeId: node.id,
                        from: currentInputHandles.length,
                        to: newNumberOfHandles,
                    });
                    const nodeWithNewHandles: RuleEditorNode = {
                        ...node,
                        data: {
                            ...node.data,
                            handles: [...utils.nonInputHandles(node), ...utils.createInputHandles(newNumberOfHandles)],
                        },
                    };
                    // This need to be done every time the handles of a node have been changed, else the UI does not show the current state
                    setTimeout(() => {
                        updateNodeInternals(node.id);
                    }, 1);
                    return nodeWithNewHandles;
                } else {
                    return node;
                }
            } else {
                return elem;
            }
        });
        // Add handle change actions to change stack
        if (handleChangeOperations.length > 0) {
            addRuleModelChange({ operations: handleChangeOperations }, false);
        }
        return changedElements;
    };

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
            case "Change number of input handles":
                return {
                    type: "Change number of input handles",
                    nodeId: undoOp.nodeId,
                    from: undoOp.to,
                    to: undoOp.from,
                };
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
            changeElementsInternal((els) => {
                let currentElements = els;
                reversedChanges.forEach((change) => {
                    currentElements = executeRuleModelChangeInternal(change, currentElements, true);
                });
                return currentElements;
            });
            resetSelectedElements();
        }
        if (ruleUndoStack.length === 0 || !ruleUndoStack.find((change) => change !== "Transaction boundary")) {
            // If stack is empty or only transaction markers exist
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
            changeElementsInternal((els) => {
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
        if (readOnlyState.enabled) {
            return;
        }
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
        if (readOnlyState.enabled) {
            return currentElements;
        }
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
                    triggerQuickEvaluation();
                    break;
                case "Delete node":
                    changedElements = deleteElementsInternal(
                        groupedChange.map((change) => (change as DeleteNode).node),
                        changedElements
                    );
                    triggerQuickEvaluation();
                    break;
                case "Delete edge":
                    changedElements = deleteElementsInternal(
                        groupedChange.map((change) => (change as DeleteEdge).edge),
                        changedElements
                    );
                    triggerQuickEvaluation();
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
                case "Change number of input handles":
                    const nodesInputHandlesDiff: Map<string, number> = new Map();
                    groupedChange.forEach((change) => {
                        const inputHandlesChange = change as ChangeNumberOfInputHandles;
                        nodesInputHandlesDiff.set(inputHandlesChange.nodeId, inputHandlesChange.to);
                    });
                    changedElements = changedElements.map((elem) => {
                        if (utils.isNode(elem) && nodesInputHandlesDiff.has(elem.id)) {
                            const node = utils.asNode(elem)!!;
                            const withoutNormalInputHandles = utils.nonInputHandles(node);
                            // This need to be done every time the handles of a node have been changed, else the UI does not show the current state
                            setTimeout(() => {
                                updateNodeInternals(node.id);
                            }, 1);
                            return {
                                ...node,
                                data: {
                                    ...node.data,
                                    handles: [
                                        ...withoutNormalInputHandles,
                                        ...utils.createInputHandles(nodesInputHandlesDiff.get(node.id)!!),
                                    ],
                                },
                            };
                        } else {
                            return elem;
                        }
                    });
                    break;
                case "Change node parameter":
                    const nodesParameterDiff: Map<string, Map<string, RuleEditorNodeParameterValue>> = new Map();
                    groupedChange.forEach((change) => {
                        const nodeChange = change as ChangeNodeParameter;
                        const nodeParameterDiff =
                            nodesParameterDiff.get(nodeChange.nodeId) ??
                            new Map<string, RuleEditorNodeParameterValue>();
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
                                        content: (adjustedProps: Partial<RuleNodeContentProps>) => (
                                            <NodeContent
                                                nodeOperations={operatorNodeOperationsInternal}
                                                nodeId={currentRuleNode.id}
                                                nodeLabel={currentRuleNode.data.label}
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
                                                showEditModal={false}
                                                {...adjustedProps}
                                            />
                                        ),
                                    },
                                };
                            } else {
                                return elem;
                            }
                        });
                    }
                    triggerQuickEvaluation();
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

    const addEdgesToNodeMapInternal = (elementsToAdd: Elements) => {
        elementsToAdd.forEach((elem) => {
            if (utils.isEdge(elem)) {
                const edge = utils.asEdge(elem)!!;
                const targetIdx = Number.parseInt(edge.targetHandle ?? "");
                if (
                    Number.isInteger(targetIdx) &&
                    targetIdx >= 0 &&
                    nodeMap.has(edge.source) &&
                    nodeMap.has(edge.target)
                ) {
                    nodeMap.get(edge.source)!!.output = edge.target;
                    const inputArray = nodeMap.get(edge.target)!!.inputs;
                    inputArray[targetIdx] = edge.source;
                }
            }
        });
    };

    const deleteEdgesFromNodeMapInternal = (elementsToDelete: Elements) => {
        elementsToDelete.forEach((elem) => {
            if (utils.isEdge(elem)) {
                const edge = utils.asEdge(elem)!!;
                const targetIdx = Number.parseInt(edge.targetHandle ?? "");
                if (
                    Number.isInteger(targetIdx) &&
                    targetIdx >= 0 &&
                    nodeMap.has(edge.source) &&
                    nodeMap.has(edge.target)
                ) {
                    nodeMap.get(edge.source)!!.output = undefined;
                    const inputArray = nodeMap.get(edge.target)!!.inputs;
                    inputArray[targetIdx] = undefined;
                    utils.adaptInputArray(inputArray);
                }
            }
        });
    };

    /**
     * Functions that change the react-flow model state.
     **/
    // Add new elements to react-flow model
    const addElementsInternal = (elementsToAdd: Elements, els: Elements): Elements => {
        addEdgesToNodeMapInternal(elementsToAdd);
        return [...els, ...elementsToAdd];
    };
    // Delete multiple elements and return updated elements array
    const deleteElementsInternal = (elementsToRemove: Elements, els: Elements): Elements => {
        deleteEdgesFromNodeMapInternal(elementsToRemove);
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
        nodeParametersToChange: Map<string, Map<string, RuleEditorNodeParameterValue>>
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
    };

    // Creates a new node and setups all relevant helper data structures
    const createNodeInternal = (
        ruleOperator: IRuleOperator,
        position: XYPosition,
        overwriteParameterValues?: RuleOperatorNodeParameters
    ): RuleEditorNode | undefined => {
        if (reactFlowInstance && ruleEditorContext.operatorSpec) {
            const ruleNode = ruleEditorContext.convertRuleOperatorToRuleNode(ruleOperator);
            ruleNode.position = position;
            ruleNode.parameters = { ...ruleNode.parameters, ...overwriteParameterValues };
            const newNode = utils.createNewOperatorNode(
                ruleNode,
                operatorNodeOperationsInternal,
                operatorNodeCreateContextInternal(
                    ruleOperator.pluginId,
                    reactFlowInstance,
                    ruleEditorContext.operatorSpec!!
                )
            );
            nodeMap.set(newNode.id, {
                node: { ...ruleNode, nodeId: newNode.id },
                inputs: [],
                output: undefined,
            });
            return newNode;
        } else {
            console.warn(
                "Could not create node since React flow and rule operator specifications are not initialized yet!",
                ruleOperator
            );
        }
    };

    /**
     * Public interface model change functions.
     *
     * All public change functions must call addAndExecuteRuleModelChangeInternal in order to register and execute the changes.
     * The must not change model elements directly.
     **/

    /** Add a new node from the rule operators list. */
    const addNode = (
        ruleOperator: IRuleOperator,
        position: XYPosition,
        overwriteParameterValues?: RuleOperatorNodeParameters
    ) => {
        const newNode = createNodeInternal(ruleOperator, position, overwriteParameterValues);
        if (newNode) {
            changeElementsInternal((els) => {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addNode(newNode), els);
            });
        } else {
            console.warn("No new node has been created.", ruleOperator, position, overwriteParameterValues);
        }
    };

    const fetchRuleOperatorByPluginId = (pluginId: string, pluginType: string): IRuleOperator | undefined => {
        const op = (ruleEditorContext.operatorList ?? []).find(
            (op) => op.pluginType === pluginType && op.pluginId === pluginId
        );
        if (op) {
            return op;
        } else {
            console.warn(`Operator with plugin type '${pluginType}' and plugin ID '${pluginId}' does not exist!`);
        }
    };

    /** Create a new rule editor node of a specific operator plugin
     *
     * @param pluginType Type of the plugin
     * @param pluginId   ID of the plugin
     * @param position   Position on the canvas.
     * @param overwriteParameterValues If initial parameters should be overwritten by different values.
     * @param isCanvasPosition If the given position is the position of the currently displayed canvas, else it is the node's real position.
     */
    const addNodeByPlugin = (
        pluginType: string,
        pluginId: string,
        position: XYPosition,
        overwriteParameterValues?: RuleOperatorNodeParameters,
        isCanvasPosition: boolean = false
    ) => {
        const realPosition = isCanvasPosition && reactFlowInstance ? reactFlowInstance.project(position) : position;
        const op = fetchRuleOperatorByPluginId(pluginId, pluginType);
        if (op) {
            addNode(op, realPosition, overwriteParameterValues);
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
        changeElementsInternal((els) => {
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
        }, true);
    };

    /** Delete multiple nodes, e.g. from a selection. */
    const deleteNodes = (nodeIds: string[]) => {
        changeElementsInternal((els) => {
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
        }, true);
    };

    /** Add a new edge.
     *
     * @param sourceNodeId Connect from this node...
     * @param targetNodeId ...to this node.
     * @param targetHandleId The input port of the target node.
     * @param previousTargetHandle If this is a swap operation this is the previously used handle. If an existing edge was connected
     *                             to the target handle, it will be swapped  to the previous handle.
     */
    const addEdge = (
        sourceNodeId: string,
        targetNodeId: string,
        targetHandleId: string | undefined,
        previousTargetHandle?: string
    ) => {
        if (targetHandleId && !isValidEdge(sourceNodeId, targetNodeId, targetHandleId)) {
            return;
        }
        changeElementsInternal((els) => {
            let currentElements = els;
            let toTargetHandleId: string | undefined = targetHandleId;
            if (!targetHandleId) {
                // If the target handle is not defined, connect to the first empty handle
                const node = utils.nodeById(els, targetNodeId);
                if (!node) {
                    return currentElements;
                }
                const inputHandles = (node!!.data.handles ?? []).filter(
                    (handle) => handle.type === "target" && !handle.category
                );
                const existingConnections = utils.findEdges({ elements: currentElements, target: targetNodeId });
                const occupiedHandles = new Set<string | null | undefined>(
                    existingConnections.map((edge) => edge.targetHandle)
                );
                const freeHandle = inputHandles.find(
                    (handle) =>
                        handle.id &&
                        !occupiedHandles.has(handle.id) &&
                        isValidEdge(sourceNodeId, targetNodeId, handle.id)
                );
                if (freeHandle) {
                    // Connect to free handle
                    toTargetHandleId = freeHandle.id;
                } else {
                    // No free handle exists, do nothing
                    return currentElements;
                }
            }
            // Remove existing edges to the same target port and from the same source node
            const existingEdgesToSameNodeHandle = targetHandleId
                ? utils.findEdges({
                      elements: currentElements,
                      target: targetNodeId,
                      targetHandle: targetHandleId,
                  })
                : [];
            const existingEdgesFromSameSource = utils
                .findEdges({
                    elements: currentElements,
                    source: sourceNodeId,
                })
                .filter((edge) => !existingEdgesToSameNodeHandle.find((otherEdge) => edge.id === otherEdge.id));
            const existingEdges = [...existingEdgesToSameNodeHandle, ...existingEdgesFromSameSource];
            if (existingEdges.length > 0) {
                currentElements = addAndExecuteRuleModelChangeInternal(
                    RuleModelChangesFactory.deleteEdges(existingEdges),
                    currentElements
                );
            }
            // Swap existing edge if it was connected to the same target handle
            if (existingEdgesToSameNodeHandle.length > 0 && previousTargetHandle) {
                currentElements = addAndExecuteRuleModelChangeInternal(
                    RuleModelChangesFactory.addEdge({
                        ...existingEdgesToSameNodeHandle[0],
                        targetHandle: previousTargetHandle,
                    }),
                    currentElements
                );
            }
            const edge = utils.createEdge(
                sourceNodeId,
                targetNodeId,
                toTargetHandleId!!,
                edgeType(nodeMap.get(sourceNodeId)?.node)
            );
            return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addEdge(edge), currentElements);
        }, true);
    };

    /** Deletes a single edge. */
    const deleteEdge = (edgeId: string, updateHandles?: boolean) => {
        changeElementsInternal((els) => {
            const edge = utils.edgeById(els, edgeId);
            if (edge) {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteEdge(edge), els);
            } else {
                return els;
            }
        }, updateHandles ?? true);
    };

    /** Delete multiple edges. */
    const deleteEdges = (edgeIds: string[]) => {
        changeElementsInternal((els) => {
            const edges = utils.edgesById(els, edgeIds);
            if (edges.length > 0) {
                return addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.deleteEdges(edges), els);
            } else {
                return els;
            }
        }, true);
    };

    /** Copy and paste nodes with a given offset. */
    const copyAndPasteNodes = (nodeIds: string[], offset: XYPosition) => {
        changeElementsInternal((els) => {
            const originalNodes = utils.nodesById(els, nodeIds);
            const nodeIdMap = new Map<string, string>();
            const newNodes: RuleEditorNode[] = [];
            originalNodes.forEach((node) => {
                const origRuleOperatorNode = node.data.businessData.originalRuleOperatorNode;
                const position = { x: node.position.x + offset.x, y: node.position.y + offset.y };
                const op = fetchRuleOperatorByPluginId(origRuleOperatorNode.pluginId, origRuleOperatorNode.pluginType);
                if (op) {
                    const newNode = createNodeInternal(
                        op,
                        position,
                        Object.fromEntries(nodeParameters.get(node.id) ?? new Map())
                    );
                    if (newNode) {
                        nodeIdMap.set(node.id, newNode.id);
                        newNodes.push(newNode);
                    }
                }
            });
            const newEdges: Edge[] = [];
            els.forEach((elem) => {
                if (utils.isEdge(elem)) {
                    const edge = utils.asEdge(elem)!!;
                    if (nodeIdMap.has(edge.source) && nodeIdMap.has(edge.target)) {
                        const newEdge = utils.createEdge(
                            nodeIdMap.get(edge.source)!!,
                            nodeIdMap.get(edge.target)!!,
                            edge.targetHandle!!,
                            edge.type ?? "step"
                        );
                        newEdges.push(newEdge);
                    }
                }
            });
            const withNodes = addAndExecuteRuleModelChangeInternal(RuleModelChangesFactory.addNodes(newNodes), els);
            resetSelectedElements();
            setTimeout(() => {
                unsetUserSelection();
                setSelectedElements([...newNodes, ...newEdges]);
            }, 100);
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

    const currentParameterValue = (nodeId: string, parameterId: string): RuleEditorNodeParameterValue => {
        const nodeDiff = nodeParameters.get(nodeId);
        if (nodeDiff) {
            return nodeDiff.get(parameterId);
        } else {
            // Node parameters must be initialized at this point
            console.warn("No parameters for node " + nodeId + " exist!");
        }
    };

    /** Change multiple node parameters in one transaction */
    const changeNodeParametersSingleTransaction = (nodeId: string, parameterValues: Map<string, string>) => {
        startChangeTransaction();
        const nodeParameterValues = nodeParameters.get(nodeId);
        if (nodeParameterValues) {
            const actions: RuleModelChanges = {
                operations: [...parameterValues.entries()].map(([paramId, value]) => {
                    return {
                        type: "Change node parameter",
                        nodeId,
                        parameterId: paramId,
                        from: nodeParameterValues.get(paramId),
                        to: value,
                    };
                }),
            };
            addAndExecuteRuleModelChangeInternal(actions, []);
        }
    };

    /** Changes a single node parameter to a new value. */
    const changeNodeParameter = (
        nodeId: string,
        parameterId: string,
        newValue: RuleEditorNodeParameterValue,
        autoStartTransaction: boolean = true
    ) => {
        const changeValue = (from: RuleEditorNodeParameterValue, to: RuleEditorNodeParameterValue) => {
            // This does not change the actual elements, so we provide dummy elements
            addAndExecuteRuleModelChangeInternal(
                RuleModelChangesFactory.changeNodeParameter(nodeId, parameterId, from, to),
                []
            );
        };
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
            changeValue(recentParameterChange!!.from, newValue);
        } else {
            changeValue(currentParameterValue(nodeId, parameterId), newValue);
        }
    };

    /** Move a node to a new position. */
    const moveNode = (nodeId: string, newPosition: XYPosition) => {
        changeElementsInternal((els) => {
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

    /** Moves multiple node sby a given offset. */
    const moveNodes = (nodeIds: string[], offset: XYPosition) => {
        changeElementsInternal((els) => {
            const nodes = utils.nodesById(els, nodeIds);
            if (nodes.length > 0) {
                const changes: ChangeNodePosition[] = nodes.map((node) => {
                    return {
                        type: "Change node position",
                        nodeId: node.id,
                        from: node.position,
                        to: { x: node.position.x + offset.x, y: node.position.y + offset.y },
                    };
                });
                return addAndExecuteRuleModelChangeInternal({ operations: changes }, els);
            } else {
                return els;
            }
        });
    };

    /**
     * centers a rule node in canvas
     */
    const centerNodeInCanvas = (node: RuleEditorNode) => {
        const instanceState = reactFlowInstance?.toObject();
        const x = node.position.x + 100;
        const y = node.position.y + 50;
        setCenter(x, y, instanceState?.zoom ?? 1);
    };

    /** Center the node by ID. */
    const centerNode = (nodeId: string): boolean => {
        const node = utils.nodeById(current.elements, nodeId);
        if (node) {
            centerNodeInCanvas(node);
            return true;
        } else {
            return false;
        }
    };

    /**
     * Adds highlighting for all matching nodes in the canvas and optionally removed existing highlighting.
     */
    const highlightNodes = (
        nodeIds: string[],
        highlightState: HighlightingState,
        removeExistingHighlighting: boolean
    ) => {
        const currentHighlighting = (node: RuleEditorNode): HighlightingState[] =>
            typeof node.data.highlightedState === "string"
                ? [node.data.highlightedState]
                : node.data.highlightedState ?? [];
        const nodeIdSet = new Set(nodeIds);
        changeElementsInternal((elements) =>
            elements.map((el) => {
                if (utils.isNode(el)) {
                    const node = utils.asNode(el)!!;
                    if (nodeIdSet.has(node.id)) {
                        node.data = {
                            ...node.data,
                            highlightedState: removeExistingHighlighting
                                ? [highlightState]
                                : [...currentHighlighting(node), highlightState],
                        };
                    } else if (removeExistingHighlighting) {
                        node.data = {
                            ...node.data,
                            highlightedState: undefined,
                        };
                    }
                }
                return el;
            })
        );
    };

    const clearHighlighting = () => {
        changeElementsInternal((elements) =>
            elements.map((el) => {
                if (utils.isNode(el)) {
                    const node = utils.asNode(el)!!;
                    node.data = {
                        ...node.data,
                        highlightedState: undefined,
                    };
                }
                return el;
            })
        );
    };

    const fixNodeInputs = () => {
        changeElementsInternal((els) => {
            return els;
        }, true);
    };

    /** Layout the rule nodes, since this must be async it cannot return the new elements directly in the setElements function. */
    const autoLayoutInternal = async (
        elements: Elements,
        addChangeHistory: boolean,
        startTransaction: boolean
    ): Promise<Elements> => {
        const newLayout = utils.autoLayout(elements, zoom);
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
        return new Promise<Elements>((resolve) => {
            if (changeNodePositionOperations.length > 0) {
                if (startTransaction) {
                    startChangeTransaction();
                }
                const changeNodePositions = { operations: changeNodePositionOperations };
                changeElementsInternal((elems) => {
                    const newElements = addChangeHistory
                        ? addAndExecuteRuleModelChangeInternal(changeNodePositions, elements)
                        : executeRuleModelChangeInternal(changeNodePositions, elements);
                    resolve(newElements);
                    return newElements;
                });
            } else {
                resolve(elements);
            }
        });
    };

    const operatorNodeOperationsInternal: IOperatorNodeOperations = {
        handleDeleteNode: deleteNode,
        handleParameterChange: changeNodeParameter,
    };

    const nodePluginId = (nodeId: string) => nodeMap.get(nodeId)?.node.pluginId;

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
        initParameters: initNodeParametersInternal,
        isValidConnection,
        nodePluginId,
        ruleEvaluationContext,
        updateNodeParameters: changeNodeParametersSingleTransaction,
    });

    /** Auto-layout the rule nodes.
     */
    const autoLayout = (startTransaction: boolean = true) => {
        autoLayoutInternal(current.elements, true, startTransaction);
    };

    /** Convert to rule operator nodes. Only this representation should be handed outside of this component. */
    const ruleOperatorNodes = (): IRuleOperatorNode[] => {
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
        current.elements.forEach((elem) => {
            if (utils.isNode(elem)) {
                nodes.push(utils.asNode(elem)!!);
            } else {
                const edge = utils.asEdge(elem)!!;
                inputEdgesByNodeId(edge.target).push(edge);
            }
        });
        return nodes.map((node) => {
            const inputHandleIds = utils.inputHandles(node).map((h) => h.id!!);
            const inputEdges = nodeInputEdges.get(node.id) ?? [];
            const inputEdgeMap = new Map(inputEdges.map((e) => [e.targetHandle, e.source]));
            const inputs = inputHandleIds.map((handleId) => inputEdgeMap.get(handleId));
            const portSpec = node.data.businessData.originalRuleOperatorNode.portSpecification;
            // Remove undefined input ports above last defined input if spec allows it
            if (!portSpec.maxInputPorts) {
                while (inputs.length > Math.max(portSpec.minInputPorts - 1, 0) && inputs[inputs.length - 1] == null) {
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
                              const value =
                                  parameterDiff && parameterDiff.has(parameterId)
                                      ? parameterDiff.get(parameterId)
                                      : parameterValue;
                              return [parameterId, typeof value === "string" ? value : value ? value.value : undefined];
                          })
                      )
                    : originalNode.parameters,
                pluginId: originalNode.pluginId,
                pluginType: originalNode.pluginType,
                portSpecification: originalNode.portSpecification,
                position: node.position,
                description: originalNode.description,
            };
            return ruleOperatorNode;
        });
    };

    /** Save the current rule. */
    const saveRule = async () => {
        const saveResult = await ruleEditorContext.saveRule(ruleOperatorNodes());
        if (saveResult.success) {
            // Reset UNDO state
            ruleUndoStack.splice(0);
            setCanUndo(false);
        }
        if ((saveResult.nodeErrors ?? []).length > 0) {
            const firstNodeError = saveResult.nodeErrors!![0];
            const node = utils.nodeById(elements, firstNodeError.nodeId);
            if (node) {
                centerNodeInCanvas(node);
                highlightNodes(
                    saveResult.nodeErrors!!.map((err) => err.nodeId),
                    "danger",
                    true
                );
            }
        } else {
            clearHighlighting();
        }
        ruleEvaluationContext.clearRuleValidationError();
        return saveResult.success;
    };

    const initModel = async () => {
        const handleDeleteNode = (nodeId: string) => {
            startChangeTransaction();
            deleteNode(nodeId);
        };
        nodeMap.clear();
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
        // Init node map for edgeType, set inputs and output further below
        operatorsNodes!!.forEach((opNode) =>
            nodeMap.set(opNode.nodeId, { node: opNode, inputs: [], output: undefined })
        );
        // Create edges
        const edges: Edge[] = [];
        // Mapping from source to target node. Each source node can only have on connection to a target node.
        const targetNode = new Map<string, string>();
        operatorsNodes!!.forEach((node) => {
            node.inputs.forEach((inputNodeId, idx) => {
                if (inputNodeId) {
                    // Edge IDs do not currently matter
                    edges.push(
                        utils.createEdge(inputNodeId, node.nodeId, `${idx}`, edgeType(nodeMap.get(inputNodeId)?.node))
                    );
                    targetNode.set(inputNodeId, node.nodeId);
                }
            });
        });

        // Set helper data-structures for fast access to operator data
        operatorsNodes!!.forEach((opNode) =>
            nodeMap.set(opNode.nodeId, {
                node: opNode,
                inputs: opNode.inputs,
                output: targetNode.get(opNode.nodeId),
            })
        );
        let elems: Elements = [...nodes, ...edges];
        if (needsLayout) {
            elems = await autoLayoutInternal(elems, false, false);
        }
        setElements(elems);
        utils.initNodeBaseIds(nodes);
        ruleUndoStack.splice(0);
        ruleRedoStack.splice(0);
        // Center and then zoom not too far out
        setTimeout(() => {
            reactFlowInstance?.fitView();
            reactFlowInstance?.zoomTo(0.75);
        }, 1);
    };

    return (
        <RuleEditorModelContext.Provider
            value={{
                elements,
                isReadOnly: () => readOnlyState.enabled,
                readOnly,
                setIsReadOnly: ruleEditorContext.readOnlyMode ? undefined : setIsReadOnly,
                setReactFlowInstance,
                saveRule,
                undo,
                canUndo,
                redo,
                canRedo,
                executeModelEditOperation: {
                    startChangeTransaction,
                    addNode,
                    addNodeByPlugin,
                    deleteNode,
                    deleteNodes,
                    copyAndPasteNodes,
                    moveNode,
                    changeNodeParameter,
                    addEdge,
                    deleteEdge,
                    autoLayout,
                    deleteEdges,
                    moveNodes,
                    fixNodeInputs,
                },
                unsavedChanges: canUndo,
                isValidEdge,
                centerNode,
                ruleOperatorNodes,
            }}
        >
            {children}
        </RuleEditorModelContext.Provider>
    );
};
