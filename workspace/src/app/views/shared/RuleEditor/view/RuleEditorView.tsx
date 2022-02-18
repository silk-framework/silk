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
import { RuleEditorOperatorSidebar } from "./RuleEditorOperatorSidebar";
import React, { MouseEvent as ReactMouseEvent } from "react";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { Connection, Elements, Node, OnConnectStartParams, XYPosition } from "react-flow-renderer/dist/types";
import {
    IRuleEditorViewConnectState,
    IRuleEditorViewDragState,
    IRuleEditorViewSelectionDragState,
} from "./RuleEditorView.typings";
import { ruleEditorModelUtilsFactory } from "../model/RuleEditorModel.utils";

//snap grid
const snapGrid: [number, number] = [15, 15];
const modelUtils = ruleEditorModelUtilsFactory();

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

    /** Handle moving a node. */
    const handleNodeDragStart = (event: React.MouseEvent<Element, MouseEvent>, node: Node) => {
        dragState.nodeDragStartPosition = node.position;
    };
    /** Handle moving a node. */
    const handleNodeDragStop = (event: React.MouseEvent<Element, MouseEvent>, node: Node) => {
        if (dragState.nodeDragStartPosition) {
            modelContext.executeModelEditOperation.startChangeTransaction();
            modelContext.executeModelEditOperation.moveNode(node.id, node.position);
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
            modelContext.executeModelEditOperation.deleteEdge(originalEdgeId);
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
                modelContext.executeModelEditOperation.startChangeTransaction();
                nodes.forEach((node) => {
                    modelContext.executeModelEditOperation.moveNode(node.id, node.position);
                });
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

    // Triggered after the react-flow instance has been loaded
    const onLoad = (_reactFlowInstance: OnLoadParams) => {
        setReactFlowInstance(_reactFlowInstance);
        modelContext.setReactFlowInstance(_reactFlowInstance);
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
                        // onElementClick={onElementClick}
                        onSelectionDragStart={handleSelectionDragStart}
                        onSelectionDragStop={handleSelectionDragStop}
                        onElementsRemove={onElementsRemove}
                        onConnectStart={onConnectStart}
                        onConnect={onConnect}
                        onConnectEnd={onConnectEnd}
                        onNodeDragStart={handleNodeDragStart}
                        onNodeDragStop={handleNodeDragStop}
                        onNodeMouseEnter={onNodeMouseEnter}
                        onNodeMouseLeave={onNodeMouseLeave}
                        onLoad={onLoad}
                        // onDrop={onDrop}
                        // onDragOver={onDragOver}
                        // nodeTypes={nodeTypes}
                        // edgeTypes={edgeTypes}
                        onEdgeUpdateStart={onEdgeUpdateStart}
                        onEdgeUpdateEnd={onEdgeUpdateEnd}
                        onEdgeUpdate={onEdgeUpdate}
                        nodeTypes={nodeTypes}
                        edgeTypes={edgeTypes}
                        connectionLineType={ConnectionLineType.Step}
                        className="eccapp-di__floweditor__graph"
                        snapGrid={snapGrid}
                        snapToGrid={true}
                        zoomOnDoubleClick={false}
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
                    {/*{edgeTools}*/}
                </GridColumn>
            </GridRow>
        </Grid>
    );
};
