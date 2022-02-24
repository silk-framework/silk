import { XYPosition } from "react-flow-renderer";
import { OnConnectStartParams } from "react-flow-renderer/dist/types";

export interface IRuleEditorViewDragState {
    nodeDragStartPosition?: XYPosition | undefined;
}

export interface IRuleEditorViewSelectionDragState {
    // The positions of the dragged nodes
    selectionStartPositions: Map<string, XYPosition>;
}

export interface IRuleEditorViewConnectState {
    // If there is an active connect operation going on, i.e. the user is currently creating a new connection
    connectOperationActive: boolean;
    // If the mouse is over a node
    overNode?: string;
    // If the edge has already been connected to a handle
    edgeConnected: boolean;
    // The parameters of the current connection action
    connectParams?: OnConnectStartParams;
}
