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
    OnLoadParams,
} from "react-flow-renderer";
import { RuleEditorOperatorSidebar } from "./sidebar/RuleEditorOperatorSidebar";
import React from "react";
import { RuleEditorModelContext } from "../contexts/RuleEditorModelContext";
import { Node } from "react-flow-renderer/dist/types";
import { IRuleEditorViewDragState } from "./RuleEditorView.typings";

//snap grid
const snapGrid: [number, number] = [15, 15];

export const RuleEditorView = () => {
    const reactFlowWrapper = React.useRef<any>(null);
    const [reactFlowInstance, setReactFlowInstance] = React.useState<OnLoadParams | undefined>(undefined);
    const [dragState] = React.useState<IRuleEditorViewDragState>({});
    const modelContext = React.useContext(RuleEditorModelContext);

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

    // Triggered after the react-flow instance has been loaded
    const onLoad = (_reactFlowInstance: OnLoadParams) => {
        setReactFlowInstance(_reactFlowInstance);
        modelContext.setReactFlowInstance(_reactFlowInstance);
    };

    // Add new node when operator is dropped
    const onDrop = (e: React.DragEvent<HTMLDivElement>) => {
        console.log("Dropping node");
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
                        // onElementClick={onElementClick}
                        // onSelectionDragStart={handleSelectionDragStart}
                        // onSelectionDragStop={handleSelectionDragStop}
                        // onElementsRemove={onElementsRemove}
                        // onConnect={onConnect}
                        onNodeDragStart={handleNodeDragStart}
                        onNodeDragStop={handleNodeDragStop}
                        onLoad={onLoad}
                        onDrop={onDrop}
                        onDragOver={onDragOver}
                        // nodeTypes={nodeTypes}
                        // edgeTypes={edgeTypes}
                        // onEdgeUpdateStart={onEdgeUpdateStart}
                        // onEdgeUpdateEnd={onEdgeUpdateEnd}
                        // onEdgeUpdate={onEdgeUpdate}
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
