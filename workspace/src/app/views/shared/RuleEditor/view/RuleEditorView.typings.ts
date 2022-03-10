import { XYPosition } from "react-flow-renderer";
import { Node, OnConnectStartParams } from "react-flow-renderer/dist/types";
import { RuleEditorNode } from "../model/RuleEditorModel.typings";

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
    overNode?: Node;
    // If the edge has already been connected to a handle
    edgeConnected: boolean;
    // The parameters of the current connection action
    connectParams?: OnConnectStartParams;
}
