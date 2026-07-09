import type {
    RuleClipboardTask,
    RuleClipboardTaskData,
    RuleNodeCopySerialization,
} from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import { ruleEditorNodeParameterValue } from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import type { RuleEditorPatchableNodeProjection } from "../../shared/RuleEditor/RuleEditor.typings";
import type { IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import type { RuleBlockPort } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import i18next from "i18next";

/** Rule-block-specific clipboard additions that travel alongside the generic copied node subtree. */
export interface RuleBlockClipboardEditorData {
    inputPorts: RuleBlockPort[];
}

/** Result of rewriting a generic clipboard subtree into a rule-block-compatible paste payload. */
export interface RuleBlockPastePreparationResult {
    taskData: RuleClipboardTaskData;
    createdPorts: RuleBlockPort[];
}

interface CollectRuleBlockClipboardCopyOptions {
    selectedNodeIds: string[];
    ruleOperatorNodes: IRuleOperatorNode[];
    existingPorts: RuleBlockPort[];
}

interface PrepareRuleBlockClipboardPasteOptions {
    currentProjectId: string;
    currentTaskId: string;
    existingPorts: RuleBlockPort[];
    createInputPortId: () => string;
    inputPortNodeMetaData: (port: RuleBlockPort) => RuleEditorPatchableNodeProjection;
}

type LinkingPasteSide = "source" | "target";
type TransformPasteNode = RuleNodeCopySerialization & { pluginType: "TransformOperator" };
type InputPortPasteNode = RuleNodeCopySerialization & { pluginType: "InputPortOperator" };
type PathPasteNode = RuleNodeCopySerialization & { pluginType: "PathInputOperator" };
type SupportedRuleBlockPasteNode = TransformPasteNode | InputPortPasteNode | PathPasteNode;

interface RuleBlockPasteState {
    portsById: Map<string, RuleBlockPort>;
    copiedPortsById: Map<string, RuleBlockPort>;
    createdPortsByPathIdentity: Map<string, RuleBlockPort>;
    createdPortsByCopiedPortId: Map<string, RuleBlockPort>;
    createdPorts: RuleBlockPort[];
    nextDisplayOrder: number;
    reuseCurrentPorts: boolean;
    createInputPortId: () => string;
    inputPortNodeMetaData: (port: RuleBlockPort) => RuleEditorPatchableNodeProjection;
}

const ALLOWED_PLUGIN_TYPES = new Set(["TransformOperator", "PathInputOperator", "InputPortOperator"]);

const isRuleBlockClipboardEditorData = (editorData: unknown): editorData is RuleBlockClipboardEditorData =>
    typeof editorData === "object" &&
    editorData != null &&
    Array.isArray((editorData as RuleBlockClipboardEditorData).inputPorts);

/** Infers the linking side of a pasted path node so source and target paths do not deduplicate into one port. */
const inputPathSide = (node: RuleNodeCopySerialization): LinkingPasteSide | undefined => {
    if (node.pluginType !== "PathInputOperator") {
        return undefined;
    }
    return node.pluginId === "targetPathInput" ? "target" : "source";
};

const normalizePathExpression = (path: string | undefined): string => path?.trim() ?? "";

/** Builds the deduplication key for path-derived input ports within a single paste operation. */
const pathPortIdentity = (node: RuleNodeCopySerialization): string => {
    const normalizedPath = normalizePathExpression(ruleEditorNodeParameterValue(node.parameters?.path));
    const side = inputPathSide(node) ?? "source";
    return normalizedPath ? `${side}:${normalizedPath}` : `${side}:__empty__:${node.nodeId}`;
};

/** Derives a readable fallback input-port label from the pasted path expression. */
const pathPortLabel = (node: RuleNodeCopySerialization, fallbackDisplayOrder: number): string => {
    const normalizedPath = normalizePathExpression(ruleEditorNodeParameterValue(node.parameters?.path));
    return normalizedPath || `Input ${fallbackDisplayOrder}`;
};

/** Rejects clipboard payloads that contain operators which are not valid inside reusable rule blocks. */
const validateSupportedRuleBlockPaste = (nodes: RuleNodeCopySerialization[]) => {
    const disallowedPlugin = nodes.find((node) => !ALLOWED_PLUGIN_TYPES.has(node.pluginType));
    if (!disallowedPlugin) {
        return;
    }
    switch (disallowedPlugin.pluginType) {
        case "ComparisonOperator":
        case "AggregationOperator":
            throw new Error(
                i18next.t("taskViews.ruleBlock.errors.pasteUnsupportedOperators"),
            );
        case "RuleBlock":
            throw new Error(i18next.t("taskViews.ruleBlock.errors.pasteNestedRuleBlocks"));
        default:
            throw new Error(
                i18next.t("taskViews.ruleBlock.errors.pasteUnsupportedOperatorType", {
                    pluginType: disallowedPlugin.pluginType,
                }),
            );
    }
};

/** Reads copied logical input-port definitions from editor-owned clipboard data, if present. */
const copiedInputPorts = (task: RuleClipboardTask): Map<string, RuleBlockPort> => {
    if (!isRuleBlockClipboardEditorData(task.editorData)) {
        return new Map();
    }
    return new Map(task.editorData.inputPorts.map((port) => [port.id, port] as const));
};

/** Detects whether the clipboard payload originates from the currently open rule block. */
const sameRuleBlockClipboardSource = (
    task: RuleClipboardTask,
    currentProjectId: string,
    currentTaskId: string,
): boolean => task.metaData.project === currentProjectId && task.metaData.task === currentTaskId;

/** Creates the mutable state used while rewriting one pasted clipboard payload. */
const createPasteState = (
    task: RuleClipboardTask,
    {
        currentProjectId,
        currentTaskId,
        existingPorts,
        createInputPortId,
        inputPortNodeMetaData,
    }: PrepareRuleBlockClipboardPasteOptions,
): RuleBlockPasteState => ({
    portsById: new Map(existingPorts.map((port) => [port.id, port] as const)),
    copiedPortsById: copiedInputPorts(task),
    createdPortsByPathIdentity: new Map(),
    createdPortsByCopiedPortId: new Map(),
    createdPorts: [],
    nextDisplayOrder:
        existingPorts.reduce((maxDisplayOrder, port) => Math.max(maxDisplayOrder, port.displayOrder), 0) + 1,
    reuseCurrentPorts: sameRuleBlockClipboardSource(task, currentProjectId, currentTaskId),
    createInputPortId,
    inputPortNodeMetaData,
});

/** Allocates a new logical input port in the destination rule block and tracks it for the final external change. */
const createGeneratedPort = (
    state: RuleBlockPasteState,
    portDefinition: Omit<RuleBlockPort, "id" | "displayOrder">,
): RuleBlockPort => {
    const createdPort: RuleBlockPort = {
        ...portDefinition,
        id: state.createInputPortId(),
        displayOrder: state.nextDisplayOrder++,
    };
    state.createdPorts.push(createdPort);
    return createdPort;
};

/** Reuses or recreates the logical input port referenced by a pasted InputPortOperator node. */
const resolveInputPortNodePort = (
    node: RuleNodeCopySerialization,
    state: RuleBlockPasteState,
): RuleBlockPort => {
    const copiedPortId = ruleEditorNodeParameterValue(node.parameters?.portId)?.trim();
    if (!copiedPortId) {
        throw new Error("Pasted input-port node is missing its logical port ID.");
    }
    if (state.reuseCurrentPorts) {
        const existingPort = state.portsById.get(copiedPortId);
        if (!existingPort) {
            throw new Error(`Pasted input-port '${copiedPortId}' does not exist in the current rule block.`);
        }
        return existingPort;
    }
    const reusedCreatedPort = state.createdPortsByCopiedPortId.get(copiedPortId);
    if (reusedCreatedPort) {
        return reusedCreatedPort;
    }
    const copiedPort = state.copiedPortsById.get(copiedPortId);
    if (!copiedPort) {
        throw new Error(`Missing copied input-port definition for pasted port '${copiedPortId}'.`);
    }
    const createdPort = createGeneratedPort(state, {
        label: copiedPort.label,
        description: copiedPort.description,
        deprecated: copiedPort.deprecated,
    });
    state.createdPortsByCopiedPortId.set(copiedPortId, createdPort);
    return createdPort;
};

/** Reuses or creates the generated logical input port that represents a pasted dataset-path leaf. */
const resolvePathDerivedPort = (
    node: RuleNodeCopySerialization,
    state: RuleBlockPasteState,
): RuleBlockPort => {
    const identity = pathPortIdentity(node);
    const reusedCreatedPort = state.createdPortsByPathIdentity.get(identity);
    if (reusedCreatedPort) {
        return reusedCreatedPort;
    }
    const createdPort = createGeneratedPort(state, {
        label: pathPortLabel(node, state.nextDisplayOrder),
        description: "",
        deprecated: false,
    });
    state.createdPortsByPathIdentity.set(identity, createdPort);
    return createdPort;
};

/** Rewrites a pasted InputPortOperator node so it points at the correct logical input port in the destination block. */
const rewriteInputPortNode = (
    node: InputPortPasteNode,
    state: RuleBlockPasteState,
): RuleNodeCopySerialization => {
    const resolvedPort = resolveInputPortNodePort(node, state);
    return {
        ...node,
        parameters: {
            portId: resolvedPort.id,
        },
        nodeMetaData: state.inputPortNodeMetaData(resolvedPort),
    };
};

/** Rewrites a pasted dataset-path leaf into a logical InputPortOperator node. */
const rewritePathNode = (
    node: PathPasteNode,
    state: RuleBlockPasteState,
): RuleNodeCopySerialization => {
    const createdPort = resolvePathDerivedPort(node, state);
    return {
        ...node,
        pluginType: "InputPortOperator" as const,
        pluginId: "inputPort" as const,
        parameters: {
            portId: createdPort.id,
        },
        nodeMetaData: state.inputPortNodeMetaData(createdPort),
    };
};

/** Dispatches the per-node rewrite logic after the supported operator allowlist has been validated. */
const rewritePastedNode = (
    node: SupportedRuleBlockPasteNode,
    state: RuleBlockPasteState,
): RuleNodeCopySerialization => {
    switch (node.pluginType) {
        case "TransformOperator":
            return node;
        case "InputPortOperator":
            return rewriteInputPortNode(node, state);
        case "PathInputOperator":
            return rewritePathNode(node, state);
    }
};

/** Collects logical input-port definitions for copied input-port nodes so external pastes can recreate them. */
const collectRuleBlockClipboardCopy = ({
    selectedNodeIds,
    ruleOperatorNodes,
    existingPorts,
}: CollectRuleBlockClipboardCopyOptions): RuleBlockClipboardEditorData | undefined => {
    const selectedNodeIdsSet = new Set(selectedNodeIds);
    const selectedInputPortIds = new Set(
        ruleOperatorNodes
            .filter((node) => selectedNodeIdsSet.has(node.nodeId) && node.pluginType === "InputPortOperator")
            .map((node) => ruleBlockUtils.requirePortId(node)),
    );
    if (selectedInputPortIds.size === 0) {
        return undefined;
    }
    const inputPorts = ruleBlockUtils
        .sortRuleBlockPorts(existingPorts)
        .filter((port) => selectedInputPortIds.has(port.id));
    return inputPorts.length > 0 ? { inputPorts } : undefined;
};

/** Validates and rewrites a pasted clipboard subtree into a rule-block-compatible node payload. */
const prepareRuleBlockClipboardPaste = (
    task: RuleClipboardTask,
    options: PrepareRuleBlockClipboardPasteOptions,
): RuleBlockPastePreparationResult => {
    validateSupportedRuleBlockPaste(task.data.nodes);
    const state = createPasteState(task, options);
    const transformedNodes: RuleNodeCopySerialization[] = task.data.nodes.map((node) => {
        const supportedNode = node as SupportedRuleBlockPasteNode;
        return rewritePastedNode(supportedNode, state);
    });

    return {
        taskData: {
            nodes: transformedNodes,
            edges: task.data.edges,
        },
        createdPorts: state.createdPorts,
    };
};

const ruleBlockPasteUtils = {
    collectRuleBlockClipboardCopy,
    prepareRuleBlockClipboardPaste,
};

export default ruleBlockPasteUtils;
