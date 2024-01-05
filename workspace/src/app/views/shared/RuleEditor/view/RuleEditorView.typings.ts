import { Edge, XYPosition } from "react-flow-renderer";
import { Node, OnConnectStartParams } from "react-flow-renderer/dist/types";
import { RuleEditorNode } from "../model/RuleEditorModel.typings";

export interface IRuleEditorViewDragState {
    nodeDragStartPosition?: XYPosition | undefined;
}

export interface IRuleEditorViewSelectionDragState {
    // The positions of the dragged nodes
    selectionStartPositions: Map<string, XYPosition>;
}

/** State that is maintained during any kind of connection based action, e.g. creating a new edge or updating an existing one. */
export interface EditorEdgeConnectionState {
    edgeConnectOperationActive: boolean;
    // Set when the edge is drawn from a source handle
    sourceNodeId?: string;
    // Set when the edge is drawn from a target handle
    targetNodeId?: string;
    // This is only set if the edge is drawn from a target handle
    targetHandleId?: string;
    // This is only set if the edge is drawn from a source handle
    sourceHandleId?: string;
    // If the mouse is over a node
    overNode?: Node;
}

/** State that is maintained during creation of a new edge. */
export interface IRuleEditorViewConnectState {
    // If there is an active connect operation going on, i.e. the user is currently creating a new connection
    connectOperationActive: boolean;
    // If the edge has already been connected to a handle
    edgeConnected: boolean;
    // The parameters of the current connection action
    connectParams?: OnConnectStartParams;
}

/** State that is maintained during the update of an existing edge. */
export interface IRuleEditorViewEdgeUpdateState {
    // Are we during an edge update
    duringEdgeUpdate: boolean;
    // Has the original edge already been deleted
    edgeDeleted: boolean;
    // The original edge
    originalEdge: Edge | undefined;
    // If a model update transaction has already been started. This is needed since the edge update covers a "delete" and an "add edge", which need to be in the same transaction.
    transactionStarted: boolean;
}
