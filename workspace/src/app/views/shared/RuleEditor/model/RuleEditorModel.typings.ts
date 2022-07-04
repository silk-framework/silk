import { Edge, Node } from "react-flow-renderer";
import { IRuleNodeData, NodeContentPropsWithBusinessData } from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import { IOperatorNodeParameterValueWithLabel } from "../../../taskViews/shared/rules/rule.typings";
import { NodeDimensions } from "@eccenca/gui-elements/src/extensions/react-flow/nodes/NodeContent";
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
    | ChangeNodeStickyContent
    | ChangeStickyNodeStyle
    | ChangeStickyNodeProperties;

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

export interface ChangeNodeSize {
    type: "Change node size";
    nodeId: string;
    from: NodeDimensions;
    to: NodeDimensions;
}

export interface ChangeStickyNodeStyle {
    type: "Change node style";
    nodeId: string;
    from: CSSProperties;
    to: CSSProperties;
}

export interface ChangeStickyNodeProperties {
    type: "Change sticky node style or content";
    nodeId: string;
    from: StickyNodePropType;
    to: StickyNodePropType;
}

export interface ChangeNodeStickyContent {
    type: "Change node text content";
    nodeId: string;
    from: string;
    to: string;
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
    changeNodeSize: (nodeId: string, from: NodeDimensions, to: NodeDimensions) =>
        toRuleModelChanges({ type: "Change node size", nodeId, from, to }),
    changeNodeStyle: (nodeId: string, from: CSSProperties, to: CSSProperties) =>
        toRuleModelChanges({ type: "Change node style", nodeId, from, to }),
    changeNodeContent: (nodeId: string, from: string, to: string) =>
        toRuleModelChanges({ type: "Change node text content", nodeId, from, to }),
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
