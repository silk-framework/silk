import {
    Background,
    BackgroundVariant,
    ConnectionLineType,
    Controls,
    Edge,
    HandleProps,
    OnLoadParams,
} from "react-flow-renderer";
import { ReactFlow } from "@eccenca/gui-elements/src/cmem";
import React, { MouseEvent as ReactMouseEvent } from "react";
import { Connection, Elements, Node, OnConnectStartParams, XYPosition } from "react-flow-renderer/dist/types";
import { SelectionMenu } from "./ruleNode/SelectionMenu";
import {
    EditorEdgeConnectionState,
    IRuleEditorViewConnectState,
    IRuleEditorViewDragState,
    IRuleEditorViewEdgeUpdateState,
    IRuleEditorViewSelectionDragState,
} from "./RuleEditorView.typings";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { EdgeMenu } from "./ruleEdge/EdgeMenu";
import { ruleEditorModelUtilsFactory, SOURCE_HANDLE_TYPE, TARGET_HANDLE_TYPE } from "../model/RuleEditorModel.utils";
import { MiniMap } from "@eccenca/gui-elements/src/extensions/react-flow/minimap/MiniMap";
import { GridColumn } from "@eccenca/gui-elements";
import { RuleEditorNode } from "../model/RuleEditorModel.typings";
import useHotKey from "../../HotKeyHandler/HotKeyHandler";
import { RuleEditorUiContext } from "../contexts/RuleEditorUiContext";
import { useSelector } from "react-redux";
import { commonSel } from "@ducks/common";
import { ReactFlowHotkeyContext } from "@eccenca/gui-elements/src/cmem/react-flow/extensions/ReactFlowHotkeyContext";

//snap grid
const snapGrid: [number, number] = [15, 15];

const modelUtils = ruleEditorModelUtilsFactory();

/** The main graphical rule editor canvas where the rule nodes are placed and connected. */
export const RuleEditorCanvas = () => {
    // Stores state during a node drag action
    const [dragState] = React.useState<IRuleEditorViewDragState>({});
    // Stores state during a selection drag action, i.e. moving a selection of nodes
    const [selectionDragState] = React.useState<IRuleEditorViewSelectionDragState>({
        selectionStartPositions: new Map(),
    });
    const modelContext = React.useContext(RuleEditorModelContext);
    const ruleEditorUiContext = React.useContext(RuleEditorUiContext);
    // Stores the state when any edge connection operation is active, i.e. creating a new or updating an existing edge
    const [edgeConnectState] = React.useState<EditorEdgeConnectionState>({
        edgeConnectOperationActive: false,
    });
    // Stores state during a connection operation, i.e. creating a new connection
    const [connectState] = React.useState<IRuleEditorViewConnectState>({
        edgeConnected: false,
        connectOperationActive: false,
    });
    // Tracks the state during an edge update operation, i.e. if it's still during an edge update and
    const [edgeUpdateState] = React.useState<IRuleEditorViewEdgeUpdateState>({
        duringEdgeUpdate: false,
        edgeDeleted: false,
        originalEdge: undefined,
        transactionStarted: false,
    });
    // Context menu that is shown on specific user actions
    const [contextMenu, setContextMenu] = React.useState<JSX.Element | null>(null);
    // At the moment react-flow's selection logic is buggy in some places, e.g. https://github.com/wbkd/react-flow/issues/1314
    // Until fixed, we will track selections ourselves and use them where bugs exist.
    const [selectionState] = React.useState<{ elements: Elements | null }>({ elements: null });
    // Needed to disable hot keys
    const { isOpen } = useSelector(commonSel.artefactModalSelector);
    const { hotKeysDisabled } = React.useContext(ReactFlowHotkeyContext);

    /** Clones the given nodes with a small offset. */
    const cloneNodes = (nodeIds: string[]) => {
        modelContext.executeModelEditOperation.startChangeTransaction();
        modelContext.executeModelEditOperation.copyAndPasteNodes(nodeIds);
    };
    useHotKey({
        hotkey: "mod+d",
        handler: (e) => {
            e.preventDefault && e.preventDefault();
            const nodeIds = selectedNodeIds();
            if (nodeIds.length > 0) {
                cloneNodes(nodeIds);
            }
        },
        enabled: !ruleEditorUiContext.modalShown && !hotKeysDisabled,
    });

    /** Selection helper methods. */
    const selectedNodeIds = (): string[] => {
        const selectedNodes = modelUtils.elementNodes(selectionState.elements ?? []);
        return selectedNodes.map((n) => n.id);
    };

    /** Handle moving a node. */
    const handleNodeDragStart = (event: React.MouseEvent<Element, MouseEvent>, node: Node) => {
        dragState.nodeDragStartPosition = node.position;
    };
    const calcOffset = (oldPosition: XYPosition, newPosition: XYPosition) => ({
        x: newPosition.x - oldPosition.x,
        y: newPosition.y - oldPosition.y,
    });
    /** Handle moving a node. */
    const handleNodeDragStop = (event: React.MouseEvent<Element, MouseEvent>, node: Node) => {
        if (dragState.nodeDragStartPosition) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            const selectedNodes = selectedNodeIds();
            const offset = calcOffset(dragState.nodeDragStartPosition, node.position);
            if (selectedNodes.length > 0) {
                // We are actually moving a selection, not a single node
                modelContext.executeModelEditOperation.moveNodes(selectedNodes, offset);
            } else {
                modelContext.executeModelEditOperation.moveNode(node.id, node.position);
            }
            dragState.nodeDragStartPosition = undefined;
        }
    };

    /** Handle changing edge connections */
    const resetEdgeConnectState = () => {
        edgeConnectState.edgeConnectOperationActive = false;
        edgeConnectState.sourceNodeId = undefined;
        edgeConnectState.targetNodeId = undefined;
        edgeConnectState.targetHandleId = undefined;
        edgeConnectState.overNode = undefined;
    };
    const startEdgeUpdateTransaction = () => {
        if (!edgeUpdateState.transactionStarted) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            edgeUpdateState.transactionStarted = true;
        }
    };
    const deleteOriginalEdgeDuringEdgeUpdate = (originalEdgeId: string) => {
        // Don't delete the original edge when the edge was not dragged, i.e. time between click and release was too small
        if (
            edgeUpdateState.duringEdgeUpdate &&
            edgeUpdateState.originalEdge?.id === originalEdgeId &&
            !edgeUpdateState.edgeDeleted
        ) {
            startEdgeUpdateTransaction();
            modelContext.executeModelEditOperation.deleteEdge(originalEdgeId, false);
            edgeUpdateState.edgeDeleted = true;
        }
    };
    // Start dragging an existing edge
    const onEdgeUpdateStart = React.useCallback((event: ReactMouseEvent, edge: Edge) => {
        edgeUpdateState.duringEdgeUpdate = true;
        edgeUpdateState.edgeDeleted = false;
        edgeUpdateState.transactionStarted = false;
        edgeUpdateState.originalEdge = edge;
        // FIXME: This is currently not possible since we do not know which side of the edge is being updated
        // see also: https://github.com/wbkd/react-flow/discussions/1961
        // initEdgeConnectState(
        //     isSourceHandle ? undefined : edge.source,
        //     isSourceHandle ? edge.target : undefined,
        //     isSourceHandle ? undefined : (edge.targetHandle ?? undefined)
        // )
        setTimeout(() => {
            // Delete the original edge, since we will start a new edge.
            deleteOriginalEdgeDuringEdgeUpdate(edge.id);
        }, 200);
    }, []);

    // Handles connecting the existing edge to a new operator/input port.
    const onEdgeUpdate = React.useCallback((oldEdge: Edge, newConnection: Connection) => {
        if (newConnection.source && newConnection.target && newConnection.targetHandle) {
            deleteOriginalEdgeDuringEdgeUpdate(oldEdge.id);
            modelContext.executeModelEditOperation.addEdge(
                newConnection.source,
                newConnection.target,
                newConnection.targetHandle,
                oldEdge.targetHandle && oldEdge.target === newConnection.target ? oldEdge.targetHandle : undefined
            );
        }
        resetEdgeConnectState();
    }, []);

    // End of an edge update independent from if the edge was connected to a new port or not
    const onEdgeUpdateEnd = React.useCallback((event, edge) => {
        edgeUpdateState.duringEdgeUpdate = false;
        resetEdgeConnectState();
        edgeUpdateState.originalEdge?.target && modelContext.executeModelEditOperation.fixNodeInputs();
    }, []);

    const initEdgeConnectState = (
        sourceNodeId: string | undefined,
        targetNodeId: string | undefined,
        targetHandleId: string | undefined
    ) => {
        edgeConnectState.edgeConnectOperationActive = true;
        edgeConnectState.sourceNodeId = sourceNodeId;
        edgeConnectState.targetNodeId = targetNodeId;
        edgeConnectState.targetHandleId = targetHandleId;
        edgeConnectState.overNode = undefined;
    };

    /** Connection logic, i.e. connecting new edges. */
    const onConnectStart = React.useCallback((e: ReactMouseEvent, params: OnConnectStartParams) => {
        e.preventDefault();
        e.stopPropagation();
        connectState.edgeConnected = false;
        connectState.connectOperationActive = true;
        connectState.connectParams = params;
        initEdgeConnectState(
            params.handleType === SOURCE_HANDLE_TYPE && params.nodeId ? params.nodeId : undefined,
            params.handleType === TARGET_HANDLE_TYPE && params.nodeId ? params.nodeId : undefined,
            params.handleId && params.handleType === TARGET_HANDLE_TYPE ? params.handleId : undefined
        );
    }, []);

    // Triggered when drawing a new connection between nodes
    const onConnect = React.useCallback((newConnection: Connection) => {
        if (
            newConnection.source &&
            newConnection.target &&
            newConnection.targetHandle &&
            newConnection.source !== newConnection.target
        ) {
            connectState.edgeConnected = true;
            modelContext.executeModelEditOperation.startChangeTransaction();
            modelContext.executeModelEditOperation.addEdge(
                newConnection.source,
                newConnection.target,
                newConnection.targetHandle
            );
        }
    }, []);

    // Connect edge to first empty port when edge was over a node and not connected to a port
    const onConnectEnd = React.useCallback((event: MouseEvent) => {
        event.preventDefault();
        event.stopPropagation();
        connectState.connectOperationActive = false;
        if (edgeConnectState.overNode) {
            disableNodeInputValidation(modelUtils.asNode(edgeConnectState.overNode)!!);
        }
        if (!connectState.edgeConnected && edgeConnectState.overNode && connectState.connectParams?.nodeId) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            modelContext.executeModelEditOperation.addEdge(
                connectState.connectParams.nodeId,
                edgeConnectState.overNode.id,
                undefined
            );
        }
        resetEdgeConnectState();
    }, []);

    // Iterate over the input handles of a node
    const iterateInputHandles = (
        ruleEditorNode: RuleEditorNode,
        handleAction: (handle: HandleProps, handleDom: Element) => void
    ) => {
        const handles = modelUtils.inputHandles(ruleEditorNode);
        const ruleDomNode = document.querySelector(`#${modelContext.canvasId} div[data-id="${ruleEditorNode.id}"]`);
        ruleDomNode &&
            handles.forEach((handle) => {
                const handleDom = ruleDomNode.querySelector(
                    `div[data-handlepos = "left"][data-handleid="${handle.id}"]`
                );
                if (handleDom) {
                    handleAction(handle, handleDom);
                }
            });
    };

    // Iterate over the output handles of a node
    const iterateOutputHandles = (
        ruleEditorNode: RuleEditorNode,
        handleAction: (handle: HandleProps, handleDom: Element) => void
    ) => {
        const handles = modelUtils.outputHandles(ruleEditorNode);
        const ruleDomNode = document.querySelector(`#${modelContext.canvasId} div[data-id="${ruleEditorNode.id}"]`);
        ruleDomNode &&
            handles.forEach((handle) => {
                const handleDom = ruleDomNode.querySelector(`div[data-handlepos = "right"]`);
                if (handleDom) {
                    handleAction(handle, handleDom);
                }
            });
    };

    const VALID_HANDLE = "valid-handle";
    const INVALID_HANDLE = "invalid-handle";

    /** Connection validation. */
    const resetEdgeValidation = (element: Element) => {
        element.classList.remove(VALID_HANDLE, INVALID_HANDLE);
    };
    const setValidEdge = (element: Element) => {
        resetEdgeValidation(element);
        element.classList.add(VALID_HANDLE);
    };
    const setInvalidEdge = (element: Element) => {
        resetEdgeValidation(element);
        element.classList.add(INVALID_HANDLE);
    };
    // Signal to the user which of the rule editor nodes handles are valid or invalid
    const enableNodeInputValidation = (ruleEditorNode: RuleEditorNode) => {
        if (edgeConnectState.sourceNodeId || edgeConnectState.targetNodeId) {
            if (edgeConnectState.targetNodeId) {
                iterateOutputHandles(ruleEditorNode, (handle, handleDom) => {
                    if (edgeConnectState.targetNodeId && edgeConnectState.targetHandleId) {
                        if (
                            modelContext.isValidEdge(
                                ruleEditorNode.id,
                                edgeConnectState.targetNodeId,
                                edgeConnectState.targetHandleId
                            )
                        ) {
                            setValidEdge(handleDom);
                        } else {
                            setInvalidEdge(handleDom);
                        }
                    }
                });
                iterateInputHandles(ruleEditorNode, (handle, handleDom) => {
                    setInvalidEdge(handleDom);
                });
            } else if (edgeConnectState.sourceNodeId) {
                iterateOutputHandles(ruleEditorNode, (handle, handleDom) => {
                    setInvalidEdge(handleDom);
                });
                iterateInputHandles(ruleEditorNode, (handle, handleDom) => {
                    if (edgeConnectState.sourceNodeId && handle.id) {
                        if (modelContext.isValidEdge(edgeConnectState.sourceNodeId, ruleEditorNode.id, handle.id)) {
                            setValidEdge(handleDom);
                        } else {
                            setInvalidEdge(handleDom);
                        }
                    }
                });
            }
        }
    };

    // Reset the handle validation
    const disableNodeInputValidation = (ruleEditorNode: RuleEditorNode) => {
        iterateInputHandles(ruleEditorNode, (handle, handleDom) => {
            resetEdgeValidation(handleDom);
        });
        iterateOutputHandles(ruleEditorNode, (handle, handleDom) => {
            resetEdgeValidation(handleDom);
        });
    };

    const onNodeMouseEnter = React.useCallback((event: ReactMouseEvent, node: Node) => {
        // Track if we are over a node during a connect operation in order to connect to the first empty port of a node
        if (edgeConnectState.edgeConnectOperationActive) {
            enableNodeInputValidation(modelUtils.asNode(node)!!);
            edgeConnectState.overNode = node;
        }
    }, []);

    const onNodeMouseLeave = React.useCallback((event: ReactMouseEvent, node: Node) => {
        disableNodeInputValidation(modelUtils.asNode(node)!!);
        edgeConnectState.overNode = undefined;
    }, []);

    /** Selection related actions. */
    // Drag a selection, i.e. move it to another position
    const handleSelectionDragStart = React.useCallback((event: ReactMouseEvent, nodes: Node[]) => {
        const nodePositionMap = new Map<string, XYPosition>();
        nodes.forEach((node) => nodePositionMap.set(node.id, node.position));
        selectionDragState.selectionStartPositions = nodePositionMap;
    }, []);

    const handleSelectionDragStop = React.useCallback((event: ReactMouseEvent, nodes: Node[]) => {
        if (nodes.length > 0) {
            const newPosition = nodes[0].position;
            const oldPosition = selectionDragState.selectionStartPositions.get(nodes[0].id);
            if (oldPosition && (newPosition.x !== oldPosition.x || newPosition.y !== oldPosition.y)) {
                const offset = { x: newPosition.x - oldPosition.x, y: newPosition.y - oldPosition.y };
                modelContext.executeModelEditOperation.startChangeTransaction();
                modelContext.executeModelEditOperation.moveNodes(selectedNodeIds(), offset);
            }
        }
    }, []);

    // Handles deletion of multiple elements, both nodes and edges
    const onElementsRemove = React.useCallback((elementsToRemove: Elements) => {
        if (elementsToRemove.length > 0) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            const edgeIds = modelUtils.elementEdges(elementsToRemove).map((e) => e.id);
            const nodesIds = modelUtils.elementNodes(elementsToRemove).map((n) => n.id);
            edgeIds.length > 0 && modelContext.executeModelEditOperation.deleteEdges(edgeIds);
            nodesIds.length > 0 && modelContext.executeModelEditOperation.deleteNodes(nodesIds);
        }
    }, []);

    /** Edge menu */
    const onEdgeContextMenu = (event: ReactMouseEvent, edge: Edge) => {
        event.preventDefault();
        showEdgeMenu(event, edge.id);
    };

    const showEdgeMenu = (event: ReactMouseEvent, edgeId: string) => {
        if (modelContext.isReadOnly()) {
            return;
        }
        setContextMenu(
            <EdgeMenu
                position={{ x: event.clientX, y: event.clientY }}
                onClose={() => setContextMenu(null)}
                removeEdge={() => {
                    modelContext.executeModelEditOperation.startChangeTransaction();
                    modelContext.executeModelEditOperation.deleteEdge(edgeId);
                }}
            />
        );
    };

    // Fired when clicked on any elements, e.g. edge or node. Used to show the edge menu.
    const onElementClick = React.useCallback((event: ReactMouseEvent, element: Node | Edge) => {
        if (modelContext.isReadOnly()) {
            return;
        }
        if (modelUtils.isEdge(element)) {
            showEdgeMenu(event, element.id);
        }
    }, []);

    /** Selection menu */
    const onSelectionContextMenu = (event: ReactMouseEvent) => {
        if (modelContext.isReadOnly()) {
            return;
        }
        // Sometimes react-flow does not provide the correct selected nodes, use tracked selection
        const nodeIds = selectedNodeIds();
        event.preventDefault();
        setContextMenu(
            <SelectionMenu
                position={{ x: event.clientX, y: event.clientY }}
                onClose={() => setContextMenu(null)}
                removeSelection={() => {
                    modelContext.executeModelEditOperation.startChangeTransaction();
                    modelContext.executeModelEditOperation.deleteNodes(nodeIds);
                }}
                cloneSelection={() => {
                    cloneNodes(nodeIds);
                }}
                copySelection={() => {
                    modelContext.executeModelEditOperation.copyNodes(nodeIds);
                }}
            />
        );
    };

    const onNodeContextMenu = (event: ReactMouseEvent, node: Node) => {
        const selectedNodes = selectedNodeIds();
        if (selectedNodes.includes(node.id)) {
            // Open selection context menu when opening context menu on selected node
            onSelectionContextMenu(event);
        }
    };

    // Track current selection
    const onSelectionChange = (elements: Elements | null) => {
        selectionState.elements = elements;
        ruleEditorUiContext.onSelection(elements);
        modelContext.updateSelectedElements(elements);
    };

    // Triggered after the react-flow instance has been loaded
    const onLoad = (_reactFlowInstance: OnLoadParams) => {
        ruleEditorUiContext.setReactFlowInstance(_reactFlowInstance);
        modelContext.setReactFlowInstance(_reactFlowInstance);
    };

    // Add new node when operator is dropped
    const onDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        const reactFlowBounds = ruleEditorUiContext?.reactFlowWrapper?.current?.getBoundingClientRect();
        const pluginData = e.dataTransfer.getData("application/reactflow");
        if (pluginData) {
            try {
                const { pluginType, pluginId, parameterValues } = JSON.parse(pluginData);
                if (pluginType && pluginId) {
                    // Position on react-flow canvas
                    const reactFlowPosition = {
                        x: e.clientX - reactFlowBounds.left - 20,
                        y: e.clientY - reactFlowBounds.top - 20,
                    };
                    modelContext.executeModelEditOperation.startChangeTransaction();
                    modelContext.executeModelEditOperation.addNodeByPlugin(
                        pluginType,
                        pluginId,
                        reactFlowPosition,
                        parameterValues,
                        true
                    );
                } else {
                    console.warn(
                        "The drag event did not contain the necessary parameters, pluginType and pluginId. Received: " +
                            pluginData
                    );
                }
            } catch (e) {
                console.warn("Could not parse drag event data. Received: " + pluginData);
            }
        } else {
            console.warn("No data in drag event. Cannot create new node!");
        }
    };

    const onDragOver = (event) => {
        event.preventDefault();
    };

    const permanentReadOnly = !!ruleEditorUiContext.showRuleOnly;
    return (
        <>
            <GridColumn>
                <ReactFlow
                    id={modelContext.canvasId}
                    data-test-id={"ruleEditor-react-flow-canvas"}
                    configuration={"linking"}
                    ref={ruleEditorUiContext?.reactFlowWrapper}
                    elements={modelContext.elements}
                    onElementClick={onElementClick}
                    onSelectionDragStart={handleSelectionDragStart}
                    onSelectionDragStop={handleSelectionDragStop}
                    onEdgeContextMenu={permanentReadOnly ? undefined : onEdgeContextMenu}
                    onElementsRemove={onElementsRemove}
                    onConnectStart={onConnectStart}
                    onConnect={onConnect}
                    onConnectEnd={onConnectEnd}
                    onNodeDragStart={handleNodeDragStart}
                    onNodeDragStop={handleNodeDragStop}
                    onNodeMouseEnter={onNodeMouseEnter}
                    onNodeMouseLeave={onNodeMouseLeave}
                    onNodeContextMenu={permanentReadOnly ? undefined : onNodeContextMenu}
                    onSelectionContextMenu={permanentReadOnly ? undefined : onSelectionContextMenu}
                    onSelectionChange={onSelectionChange}
                    onLoad={onLoad}
                    onDrop={permanentReadOnly ? undefined : onDrop}
                    onDragOver={onDragOver}
                    onEdgeUpdateStart={onEdgeUpdateStart}
                    onEdgeUpdateEnd={onEdgeUpdateEnd}
                    onEdgeUpdate={onEdgeUpdate}
                    connectionLineType={ConnectionLineType.Step}
                    snapGrid={snapGrid}
                    snapToGrid={true}
                    zoomOnDoubleClick={false}
                    minZoom={!!ruleEditorUiContext.zoomRange ? ruleEditorUiContext.zoomRange[0] : undefined}
                    maxZoom={!!ruleEditorUiContext.zoomRange ? ruleEditorUiContext.zoomRange[1] : 1.25}
                    multiSelectionKeyCode={isOpen ? null : (18 as any)} // ALT
                    deleteKeyCode={isOpen ? null : (undefined as any)}
                    scrollOnDrag={{
                        scrollStepSize: 0.1,
                        scrollInterval: 50,
                    }}
                    dropzoneFor={["application/reactflow"]}
                >
                    {!ruleEditorUiContext.hideMinimap && (
                        <MiniMap flowInstance={ruleEditorUiContext.reactFlowInstance} enableNavigation={true} />
                    )}
                    <Controls
                        showInteractive={permanentReadOnly ? false : !!modelContext.setIsReadOnly}
                        onInteractiveChange={
                            permanentReadOnly
                                ? undefined
                                : (isInteractive) =>
                                      modelContext.setIsReadOnly && modelContext.setIsReadOnly(!isInteractive)
                        }
                    />
                    <Background variant={BackgroundVariant.Lines} gap={16} />
                </ReactFlow>
                {contextMenu}
            </GridColumn>
        </>
    );
};
