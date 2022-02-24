import { Divider, Grid, GridColumn, GridRow } from "gui-elements";
import { MiniMap } from "gui-elements/src/extensions/react-flow/minimap/MiniMap";
import { edgeTypes } from "gui-elements/src/extensions/react-flow/edges/edgeTypes";
import { nodeTypes } from "gui-elements/src/extensions/react-flow/nodes/nodeTypes";
import { minimapNodeClassName, minimapNodeColor } from "gui-elements/src/extensions/react-flow/minimap/utils";
import { RuleEditorToolbar } from "./RuleEditorToolbar";
import ReactFlow, {
    Background,
    BackgroundVariant,
    ConnectionLineType,
    Controls,
    Edge,
    OnLoadParams,
} from "react-flow-renderer";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React, { MouseEvent as ReactMouseEvent } from "react";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { Connection, Elements, Node, OnConnectStartParams, XYPosition } from "react-flow-renderer/dist/types";
import {
    IRuleEditorViewConnectState,
    IRuleEditorViewDragState,
    IRuleEditorViewSelectionDragState,
} from "./RuleEditorView.typings";
import { ruleEditorModelUtilsFactory } from "../model/RuleEditorModel.utils";
import { EdgeMenu } from "./ruleEdge/EdgeMenu";
import { SelectionMenu } from "./ruleNode/SelectionMenu";

//snap grid
const snapGrid: [number, number] = [15, 15];
const modelUtils = ruleEditorModelUtilsFactory();

/** The main view of the rule editor, integrating toolbar, sidebar and main rule canvas. */
export const RuleEditorView = () => {
    const reactFlowWrapper = React.useRef<any>(null);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    // Stores state during a node drag action
    const [dragState] = React.useState<IRuleEditorViewDragState>({});
    // Stores state during a selection drag action, i.e. moving a selection of nodes
    const [selectionDragState] = React.useState<IRuleEditorViewSelectionDragState>({
        selectionStartPositions: new Map(),
    });
    const modelContext = React.useContext(RuleEditorModelContext);
    // Stores state during a connection operation
    const [connectState] = React.useState<IRuleEditorViewConnectState>({
        edgeConnected: false,
        connectOperationActive: false,
    });
    // Tracks the state during an edge update operation, i.e. if it's still during an edge update and
    const [edgeUpdateState] = React.useState<{
        duringEdgeUpdate: boolean;
        edgeDeleted: boolean;
        originalEdgeId: string | undefined;
        transactionStarted: boolean;
    }>({ duringEdgeUpdate: false, edgeDeleted: false, originalEdgeId: undefined, transactionStarted: false });
    // Context menu that is shown on specific user actions
    const [contextMenu, setContextMenu] = React.useState<JSX.Element | null>(null);
    // At the moment react-flow's selection logic is buggy in some places, e.g. https://github.com/wbkd/react-flow/issues/1314
    // Until fixed, we will track selections ourselves and use them where bugs exist.
    const [selectionState] = React.useState<{ elements: Elements | null }>({ elements: null });

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
            edgeUpdateState.originalEdgeId === originalEdgeId &&
            !edgeUpdateState.edgeDeleted
        ) {
            startEdgeUpdateTransaction();
            modelContext.executeModelEditOperation.deleteEdge(originalEdgeId, false);
            edgeUpdateState.edgeDeleted = true;
        }
    };
    // Start dragging an existing edge
    const onEdgeUpdateStart = React.useCallback((event, edge) => {
        edgeUpdateState.duringEdgeUpdate = true;
        edgeUpdateState.edgeDeleted = false;
        edgeUpdateState.transactionStarted = false;
        edgeUpdateState.originalEdgeId = edge.id;
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
    }, []);

    // End of an edge update independent from if the edge was connected to a new port or not
    const onEdgeUpdateEnd = React.useCallback((event, edge) => {
        edgeUpdateState.duringEdgeUpdate = false;
    }, []);

    /** Connection logic, i.e. connecting new edges. */
    const onConnectStart = React.useCallback((e: ReactMouseEvent, params: OnConnectStartParams) => {
        e.preventDefault();
        e.stopPropagation();
        connectState.edgeConnected = false;
        connectState.overNode = undefined;
        connectState.connectOperationActive = true;
        connectState.connectParams = params;
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
        if (!connectState.edgeConnected && connectState.overNode && connectState.connectParams?.nodeId) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            modelContext.executeModelEditOperation.addEdge(
                connectState.connectParams.nodeId,
                connectState.overNode,
                undefined
            );
        }
    }, []);

    const onNodeMouseEnter = React.useCallback((event: ReactMouseEvent, node: Node) => {
        // Track if we are over a node during a connect operation in order to connect to the first empty port of a node
        if (connectState.connectOperationActive) {
            event.preventDefault();
            event.stopPropagation();
            connectState.overNode = node.id;
        }
    }, []);

    const onNodeMouseLeave = React.useCallback(() => {
        if (connectState.connectOperationActive) {
            connectState.overNode = undefined;
        }
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
        if (modelUtils.isEdge(element)) {
            showEdgeMenu(event, element.id);
        }
    }, []);

    /** Selection menu */
    const onSelectionContextMenu = (event: ReactMouseEvent) => {
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
                    modelContext.executeModelEditOperation.copyAndPasteNodes(nodeIds, { x: 100, y: 100 });
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
    };

    // Triggered after the react-flow instance has been loaded
    const onLoad = (_reactFlowInstance: OnLoadParams) => {
        setReactFlowInstance(_reactFlowInstance);
        modelContext.setReactFlowInstance(_reactFlowInstance);
    };

    // Add new node when operator is dropped
    const onDrop = (e: React.DragEvent<HTMLDivElement>) => {
        e.preventDefault();
        const reactFlowBounds = reactFlowWrapper.current?.getBoundingClientRect();
        const pluginData = e.dataTransfer.getData("text/plain");
        if (pluginData) {
            try {
                const { pluginType, pluginId } = JSON.parse(pluginData);
                if (pluginType && pluginId) {
                    // Position on react-flow canvas
                    const reactFlowPosition = {
                        x: e.clientX - reactFlowBounds.left - 20,
                        y: e.clientY - reactFlowBounds.top - 20,
                    };
                    modelContext.executeModelEditOperation.startChangeTransaction();
                    modelContext.executeModelEditOperation.addNodeByPlugin(pluginType, pluginId, reactFlowPosition);
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

    return (
        <Grid verticalStretchable={true}>
            <GridRow>
                <GridColumn full>
                    <RuleEditorToolbar />
                    <Divider addSpacing="medium" />
                </GridColumn>
            </GridRow>
            <GridRow verticalStretched={true}>
                <GridColumn small className="eccapp-di__floweditor__sidebar">
                    <RuleEditorOperatorSidebar />
                </GridColumn>
                <GridColumn full>
                    <ReactFlow
                        data-test-id={"workflow-react-flow-canvas"}
                        ref={reactFlowWrapper}
                        elements={modelContext.elements}
                        onElementClick={onElementClick}
                        onSelectionDragStart={handleSelectionDragStart}
                        onSelectionDragStop={handleSelectionDragStop}
                        onEdgeContextMenu={onEdgeContextMenu}
                        onElementsRemove={onElementsRemove}
                        onConnectStart={onConnectStart}
                        onConnect={onConnect}
                        onConnectEnd={onConnectEnd}
                        onNodeDragStart={handleNodeDragStart}
                        onNodeDragStop={handleNodeDragStop}
                        onNodeMouseEnter={onNodeMouseEnter}
                        onNodeMouseLeave={onNodeMouseLeave}
                        onNodeContextMenu={onNodeContextMenu}
                        onSelectionContextMenu={onSelectionContextMenu}
                        onSelectionChange={onSelectionChange}
                        onLoad={onLoad}
                        onDrop={onDrop}
                        onDragOver={onDragOver}
                        // nodeTypes={nodeTypes}
                        // edgeTypes={edgeTypes}
                        onEdgeUpdateStart={onEdgeUpdateStart}
                        onEdgeUpdateEnd={onEdgeUpdateEnd}
                        onEdgeUpdate={onEdgeUpdate}
                        nodeTypes={nodeTypes}
                        edgeTypes={edgeTypes}
                        connectionLineType={ConnectionLineType.Step}
                        snapGrid={snapGrid}
                        snapToGrid={true}
                        zoomOnDoubleClick={false}
                        multiSelectionKeyCode={18} // ALT
                    >
                        <MiniMap
                            flowInstance={reactFlowInstance}
                            nodeClassName={minimapNodeClassName}
                            nodeColor={minimapNodeColor}
                            enableNavigation={true}
                        />
                        <Controls
                            showInteractive={modelContext.isReadOnly}
                            onInteractiveChange={(isInteractive) => modelContext.setIsReadOnly(!isInteractive)}
                        />
                        <Background variant={BackgroundVariant.Lines} gap={16} />
                    </ReactFlow>
                    {contextMenu}
                </GridColumn>
            </GridRow>
        </Grid>
    );
};
