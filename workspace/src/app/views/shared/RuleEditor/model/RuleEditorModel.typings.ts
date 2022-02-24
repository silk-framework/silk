import { Edge, Node } from "react-flow-renderer";
import { IRuleNodeData, NodeContentPropsWithBusinessData } from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";

export interface RuleModelChanges {
    operations: RuleModelChangeType[];
}

export interface RuleEditorNode extends Node<NodeContentPropsWithBusinessData<IRuleNodeData>> {
    data: NodeContentPropsWithBusinessData<IRuleNodeData>;
}

export type RuleModelChangeType =
    | AddNode
    | DeleteNode
    | AddEdge
    | DeleteEdge
    | ChangeNodePosition
    | ChangeNodeParameter
    | ChangeNumberOfInputHandles;

export interface AddNode {
    type: "Add node";
    node: RuleEditorNode;
}

export interface DeleteNode {
    type: "Delete node";
    node: RuleEditorNode;
}

export interface AddEdge {
    type: "Add edge";
    edge: Edge;
}

export interface DeleteEdge {
    type: "Delete edge";
    edge: Edge;
}

export interface ChangeNodePosition {
    type: "Change node position";
    nodeId: string;
    from: XYPosition;
    to: XYPosition;
}

export interface ChangeNodeParameter {
    type: "Change node parameter";
    nodeId: string;
    parameterId: string;
    from: string | undefined;
    to: string | undefined;
}

export interface ChangeNumberOfInputHandles {
    type: "Change number of input handles";
    nodeId: string;
    from: number;
    to: number;
}

// Create rule model changes action from basic change operation
const toRuleModelChanges = (ruleModelChange: RuleModelChangeType | RuleModelChangeType[]): RuleModelChanges => {
    return {
        operations: Array.isArray(ruleModelChange) ? ruleModelChange : [ruleModelChange],
    };
};

/** Convenience factory functions for rule model changes. */
export const RuleModelChangesFactory = {
    addNode: (node: RuleEditorNode): RuleModelChanges => toRuleModelChanges({ type: "Add node", node }),
    addNodes: (nodes: RuleEditorNode[]): RuleModelChanges =>
        toRuleModelChanges(
            nodes.map((node) => ({
                type: "Add node",
                node,
            }))
        ),
    deleteNode: (node: RuleEditorNode): RuleModelChanges => toRuleModelChanges({ type: "Delete node", node }),
    deleteNodes: (nodes: RuleEditorNode[]): RuleModelChanges =>
        toRuleModelChanges(
            nodes.map((node) => ({
                type: "Delete node",
                node,
            }))
        ),
    addEdge: (edge: Edge): RuleModelChanges => toRuleModelChanges({ type: "Add edge", edge }),
    addEdges: (edges: Edge[]): RuleModelChanges =>
        toRuleModelChanges(
            edges.map((edge) => ({
                type: "Add edge",
                edge,
            }))
        ),
    deleteEdge: (edge: Edge): RuleModelChanges => toRuleModelChanges({ type: "Delete edge", edge }),
    deleteEdges: (edges: Edge[]): RuleModelChanges =>
        toRuleModelChanges(
            edges.map((edge) => ({
                type: "Delete edge",
                edge,
            }))
        ),
    changeNodePosition: (nodeId: string, from: XYPosition, to: XYPosition): RuleModelChanges =>
        toRuleModelChanges({ type: "Change node position", nodeId, from, to }),
    changeNodeParameter: (
        nodeId: string,
        parameterId: string,
        from: string | undefined,
        to: string | undefined
    ): RuleModelChanges => {
        return toRuleModelChanges({ type: "Change node parameter", nodeId, parameterId, from, to });
    },
};
