import { Edge, Node } from "react-flow-renderer";
import {
    IRuleNodeData,
    IRuleOperatorNode,
    NodeContentPropsWithBusinessData,
    NodePosition,
    RuleOperatorNodeParameters,
} from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import { IOperatorNodeParameterValueWithLabel } from "../../../taskViews/shared/rules/rule.typings";
import { NodeContentProps, NodeDimensions } from "@eccenca/gui-elements";
import { CSSProperties } from "react";

export interface RuleModelChanges {
    operations: RuleModelChangeType[];
}

export interface RuleEditorNode extends Node<NodeContentPropsWithBusinessData<IRuleNodeData>> {
    data: NodeContentPropsWithBusinessData<IRuleNodeData>;
}

export type RuleEditorNodeParameterValue = IOperatorNodeParameterValueWithLabel | string | undefined;
export const ruleEditorNodeParameterValue = (value: RuleEditorNodeParameterValue): string | undefined => {
    return typeof value === "string" ? value : value?.value;
};
export const ruleEditorNodeParameterLabel = (value: RuleEditorNodeParameterValue): string | undefined => {
    return typeof value === "string" ? value : value?.label ?? value?.value;
};
export type StickyNodePropType = { content?: string; style?: CSSProperties };

export type RuleModelChangeType =
    | AddNode
    | DeleteNode
    | AddEdge
    | DeleteEdge
    | ChangeNodePosition
    | ChangeNodeParameter
    | ChangeNumberOfInputHandles
    | ChangeNodeSize
    | ChangeStickyNodeProperties;

export interface AddNode {
    type: "Add node";
    node: Node;
}

export interface DeleteNode {
    type: "Delete node";
    node: Node;
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

export interface ChangeNodeSize {
    type: "Change node size";
    nodeId: string;
    from: NodeDimensions | undefined;
    to: NodeDimensions | undefined;
}
export interface ChangeStickyNodeProperties {
    type: "Change sticky node style or content";
    nodeId: string;
    from: StickyNodePropType;
    to: StickyNodePropType;
}
export interface ChangeNodeParameter {
    type: "Change node parameter";
    nodeId: string;
    parameterId: string;
    from: RuleEditorNodeParameterValue;
    to: RuleEditorNodeParameterValue;
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
    addNode: (node: Node): RuleModelChanges => toRuleModelChanges({ type: "Add node", node }),
    addNodes: (nodes: Node[]): RuleModelChanges =>
        toRuleModelChanges(
            nodes.map((node: Node) => ({
                type: "Add node",
                node,
            }))
        ),
    deleteNode: (node: Node): RuleModelChanges => toRuleModelChanges({ type: "Delete node", node }),
    deleteNodes: (nodes: Node[]): RuleModelChanges =>
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
    changeNodeSize: (nodeId: string, from: NodeDimensions | undefined, to: NodeDimensions | undefined) =>
        toRuleModelChanges({ type: "Change node size", nodeId, from, to }),
    changeNodePosition: (nodeId: string, from: XYPosition, to: XYPosition): RuleModelChanges =>
        toRuleModelChanges({ type: "Change node position", nodeId, from, to }),
    changeStickyNodeProperties: (nodeId: string, from: StickyNodePropType, to: StickyNodePropType) =>
        toRuleModelChanges({ type: "Change sticky node style or content", nodeId, from, to }),
    changeNodeParameter: (
        nodeId: string,
        parameterId: string,
        from: RuleEditorNodeParameterValue,
        to: RuleEditorNodeParameterValue
    ): RuleModelChanges => {
        return toRuleModelChanges({ type: "Change node parameter", nodeId, parameterId, from, to });
    },
};

export interface RuleNodeCopySerialization
    extends Pick<IRuleOperatorNode, "nodeId" | "pluginId" | "pluginType" | "dimension"> {
    position: NodePosition;
    parameters?: RuleOperatorNodeParameters;
    inputHandleIds: string[];
}
