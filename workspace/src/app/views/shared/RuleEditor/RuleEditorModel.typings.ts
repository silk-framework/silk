import { Edge, Node } from "react-flow-renderer";
import { IRuleNodeData, NodeContentPropsWithBusinessData } from "./RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";

export interface RuleModelChanges {
    operations: RuleModelChangeType[];
}

export type RuleModelChangeType =
    | AddNode
    | DeleteNode
    | AddEdge
    | DeleteEdge
    | ChangeNodePosition
    | ChangeNodeParameters;

interface AddNode {
    type: "Add node";
    node: Node<NodeContentPropsWithBusinessData<IRuleNodeData>>;
}

interface DeleteNode {
    type: "Delete node";
    node: Node<NodeContentPropsWithBusinessData<IRuleNodeData>>;
}

interface AddEdge {
    type: "Add edge";
    edge: Edge;
}

interface DeleteEdge {
    type: "Delete edge";
    edge: Edge;
}

interface ChangeNodePosition {
    type: "Change node position";
    nodeId: string;
    from: XYPosition;
    to: XYPosition;
}

export interface ChangeNodeParameters {
    type: "Change node parameter";
    nodeId: string;
    parameterId: string;
    from: string | undefined;
    to: string | undefined;
}
