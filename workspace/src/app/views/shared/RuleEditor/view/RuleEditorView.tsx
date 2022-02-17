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
import React from "react";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { Connection, Node } from "react-flow-renderer/dist/types";
import { IRuleEditorViewDragState } from "./RuleEditorView.typings";

//snap grid
const snapGrid: [number, number] = [15, 15];

export const RuleEditorView = () => {
    const reactFlowWrapper = React.useRef<any>(null);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    const [dragState] = React.useState<IRuleEditorViewDragState>({});
    const modelContext = React.useContext(RuleEditorModelContext);
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
                newConnection.targetHandle
            );
        }
    }, []);

    // End of an edge update independent from if the edge was connected to a new port or not
    const onEdgeUpdateEnd = React.useCallback((event, edge) => {
        edgeUpdateState.duringEdgeUpdate = false;
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
                        // onSelectionDragStart={handleSelectionDragStart}
                        // onSelectionDragStop={handleSelectionDragStop}
                        // onElementsRemove={onElementsRemove}
                        // onConnect={onConnect}
                        onNodeDragStart={handleNodeDragStart}
                        onNodeDragStop={handleNodeDragStop}
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
