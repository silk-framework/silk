import { Edge, Node } from "react-flow-renderer";
import {
    IRuleNodeData,
    IRuleOperatorNode,
    NodeContentPropsWithBusinessData,
    NodePosition,
    RuleEditorPatchableNodeProjection,
    RuleOperatorNodeParameters,
} from "../RuleEditor.typings";
import { XYPosition } from "react-flow-renderer/dist/types";
import { IOperatorNodeParameterValueWithLabel } from "../../../taskViews/shared/rules/rule.typings";
import { NodeDimensions } from "@eccenca/gui-elements";
import { CSSProperties } from "react";

export interface RuleModelChanges {
    operations: RuleModelChangeType[];
}

export interface ExternalRuleModelChangeCallbacks {
    do: () => void;
    undo: () => void;
}

export interface RuleEditorNode extends Node<NodeContentPropsWithBusinessData<IRuleNodeData>> {
    data: NodeContentPropsWithBusinessData<IRuleNodeData>;
}

export type RuleEditorNodeParameterValue = IOperatorNodeParameterValueWithLabel | string | undefined;
export const ruleEditorNodeParameterValue = (value: RuleEditorNodeParameterValue): string | undefined => {
    return typeof value === "string" ? value : value?.value;
};
export const ruleEditorNodeParameterLabel = (value: RuleEditorNodeParameterValue): string | undefined => {
    return typeof value === "string" ? value : (value?.label ?? value?.value);
};
export type StickyNodePropType = { content?: string; style?: CSSProperties };

export type RuleModelChangeType =
    | AddNode
    | DeleteNode
    | AddEdge
    | DeleteEdge
    | ExecuteExternalRuleModelChange
    | ChangeNodePosition
    | ChangeNodeParameter
    | ChangeNodeMetaData
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

export interface ExecuteExternalRuleModelChange extends ExternalRuleModelChangeCallbacks {
    type: "Execute external rule model change";
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

export interface ChangeNodeMetaData {
    type: "Change node metadata";
    nodeId: string;
    from: RuleEditorPatchableNodeProjection;
    to: RuleEditorPatchableNodeProjection;
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
            })),
        ),
    deleteNode: (node: Node): RuleModelChanges => toRuleModelChanges({ type: "Delete node", node }),
    deleteNodes: (nodes: Node[]): RuleModelChanges =>
        toRuleModelChanges(
            nodes.map((node) => ({
                type: "Delete node",
                node,
            })),
        ),
    addEdge: (edge: Edge): RuleModelChanges => toRuleModelChanges({ type: "Add edge", edge }),
    addEdges: (edges: Edge[]): RuleModelChanges =>
        toRuleModelChanges(
            edges.map((edge) => ({
                type: "Add edge",
                edge,
            })),
        ),
    deleteEdge: (edge: Edge): RuleModelChanges => toRuleModelChanges({ type: "Delete edge", edge }),
    deleteEdges: (edges: Edge[]): RuleModelChanges =>
        toRuleModelChanges(
            edges.map((edge) => ({
                type: "Delete edge",
                edge,
            })),
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
        to: RuleEditorNodeParameterValue,
    ): RuleModelChanges => {
        return toRuleModelChanges({ type: "Change node parameter", nodeId, parameterId, from, to });
    },
    changeNodeMetaData: (
        nodeId: string,
        from: RuleEditorPatchableNodeProjection,
        to: RuleEditorPatchableNodeProjection,
    ): RuleModelChanges => {
        return toRuleModelChanges({ type: "Change node metadata", nodeId, from, to });
    },
    executeExternalRuleModelChange: (change: ExternalRuleModelChangeCallbacks): RuleModelChanges => {
        return toRuleModelChanges({ type: "Execute external rule model change", ...change });
    },
};

export interface RuleNodeCopySerialization
    extends Pick<IRuleOperatorNode, "nodeId" | "pluginId" | "pluginType" | "dimension"> {
    position: NodePosition;
    parameters?: RuleOperatorNodeParameters;
    inputHandleIds: string[];
    /** Optional inferred display metadata to apply when the pasted node is materialized. */
    nodeMetaData?: RuleEditorPatchableNodeProjection;
}

/** Generic clipboard payload for a copied rule-editor subtree. */
export interface RuleClipboardTaskData {
    nodes: RuleNodeCopySerialization[];
    edges: Partial<Edge>[];
}

/** Minimal provenance metadata attached to clipboard payloads for editor-specific reconciliation. */
export interface RuleClipboardTaskMetaData {
    domain?: string;
    project?: string;
    task?: string;
}

export interface RuleClipboardTask {
    /** Serialized canvas nodes and edges copied from the source editor. */
    data: RuleClipboardTaskData;
    /** Identifies the source editor context, e.g. to detect same-task pastes. */
    metaData: RuleClipboardTaskMetaData;
    /** Optional editor-owned data that cannot be reconstructed from canvas nodes alone. */
    editorData?: unknown;
}

/** Normalized result of an editor-specific clipboard paste preparation hook. */
export interface PreparedClipboardPaste {
    /** The final node/edge payload to materialize in the shared rule editor model. */
    taskData: RuleClipboardTaskData;
    /** Optional parent-owned state change that should participate in the same undo/redo transaction. */
    externalChange?: ExternalRuleModelChangeCallbacks;
}

/** Optional editor hook to validate, rewrite and augment clipboard pastes before node creation. */
export type PrepareClipboardPaste = (
    task: RuleClipboardTask,
) => PreparedClipboardPaste | Promise<PreparedClipboardPaste>;
