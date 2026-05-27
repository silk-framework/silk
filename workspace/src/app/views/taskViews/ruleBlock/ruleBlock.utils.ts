import { StickyNote } from "@eccenca/gui-elements";
import { IRuleOperatorNode, RuleSaveNodeError } from "../../shared/RuleEditor/RuleEditor.typings";
import { ruleEditorNodeParameterValue } from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import { IRuleBlockModel, IRuleBlockPort } from "./ruleBlock.types";

/** Creates the default empty rule block model used for new or incomplete tasks. */
const emptyRuleBlockModel = (): IRuleBlockModel => ({
    ports: [],
    layout: {
        nodePositions: {},
    },
    uiAnnotations: {
        stickyNotes: [],
    },
});

/** Normalizes optional sticky notes to the array shape expected by the editor. */
const normalizeStickyNotes = (stickyNotes?: StickyNote[]): StickyNote[] => stickyNotes ?? [];

/** Resolves the stable logical port ID for an input port node. */
const resolvePortId = (node: IRuleOperatorNode): string | undefined => {
    const portId = ruleEditorNodeParameterValue(node.parameters["portId"])?.trim();
    return portId ? portId : undefined;
};

/** Returns the logical port ID for an input port node or throws if the node is malformed. */
const requirePortId = (node: IRuleOperatorNode): string => {
    const portId = resolvePortId(node);
    if (!portId) {
        throw new Error(`InputPortOperator node '${node.nodeId}' is missing parameters.portId.`);
    }
    return portId;
};

/** Enforces the logical port invariants expected by the rule block editor runtime. */
const assertValidPorts = (ports: IRuleBlockPort[]): void => {
    const seenPortIds = new Set<string>();
    ports.forEach((port) => {
        if (!port.id.trim()) {
            throw new Error("Rule block input ports must have a non-empty ID.");
        }
        if (seenPortIds.has(port.id)) {
            throw new Error(`Rule block input port IDs must be unique. Duplicate ID '${port.id}' found.`);
        }
        seenPortIds.add(port.id);
    });
};

const portDisplayName = (port: IRuleBlockPort | undefined, fallbackId: string): string =>
    port?.label?.trim() || fallbackId;

const samePortDefinition = (left: IRuleBlockPort, right: IRuleBlockPort): boolean => {
    return (
        left.id === right.id &&
        left.label === right.label &&
        left.description === right.description &&
        left.exampleValues === right.exampleValues &&
        left.displayOrder === right.displayOrder &&
        left.deprecated === right.deprecated
    );
};

const parseDeprecated = (node: IRuleOperatorNode): boolean =>
    (ruleEditorNodeParameterValue(node.parameters["deprecated"]) ?? "").toLowerCase() === "true";

const parseDisplayOrder = (node: IRuleOperatorNode, fallback: number): number | undefined => {
    const rawValue = (ruleEditorNodeParameterValue(node.parameters["displayOrder"]) ?? "").trim();
    if (!rawValue) {
        return fallback;
    }
    const parsedValue = Number.parseInt(rawValue, 10);
    return Number.isInteger(parsedValue) ? parsedValue : undefined;
};

const nextGeneratedDisplayOrder = (ports: IRuleBlockPort[]): number =>
    ports.reduce((maxDisplayOrder, port) => Math.max(maxDisplayOrder, port.displayOrder), -1) + 1;

const sortPortDefinitions = (ports: Iterable<IRuleBlockPort>): IRuleBlockPort[] =>
    // Valid port definitions keep display orders unique. The secondary ID sort only provides deterministic order for
    // transiently duplicated or malformed states while validation is still running.
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

const orderedPortIds = (ports: Iterable<IRuleBlockPort>): string[] => sortPortDefinitions(ports).map((port) => port.id);

const duplicateDisplayOrders = (ports: Iterable<IRuleBlockPort>): number[] =>
    [...[...ports].reduce((displayOrderCount, port) => {
        displayOrderCount.set(port.displayOrder, (displayOrderCount.get(port.displayOrder) ?? 0) + 1);
        return displayOrderCount;
    }, new Map<number, number>()).entries()]
        .filter(([, count]) => count > 1)
        .map(([displayOrder]) => displayOrder)
        .sort((left, right) => left - right);

interface PortDefinitionCollectionResult {
    nodeErrors: RuleSaveNodeError[];
    portDefinitions?: IRuleBlockPort[];
}

interface UsedPortCompatibilityResult {
    errorMessage?: string;
    nodeErrors: RuleSaveNodeError[];
}

/** Returns true if a logical port belongs to the persisted rule block baseline. */
const isPersistedPort = (persistedPorts: Iterable<IRuleBlockPort>, portId: string): boolean =>
    [...persistedPorts].some((port) => port.id === portId);

/** Returns validation errors for malformed input-port nodes that are missing their logical port ID. */
const validateMissingPortIds = (
    inputPortNodes: IRuleOperatorNode[],
    missingPortIdMessage: () => string,
): RuleSaveNodeError[] =>
    inputPortNodes.flatMap((node) =>
        resolvePortId(node)
            ? []
            : [
                  {
                      nodeId: node.nodeId,
                      message: missingPortIdMessage(),
                  },
              ],
    );

/** Collects and validates persisted port definitions from the current input port nodes. */
const collectPortDefinitions = (
    persistedPorts: IRuleBlockPort[],
    inputPortNodes: IRuleOperatorNode[],
    invalidDisplayOrderMessage: string,
    conflictingPortDefinitionsMessage: (portId: string) => string,
): PortDefinitionCollectionResult => {
    const persistedPortDefinitions = new Map(persistedPorts.map((port) => [port.id, port] as const));
    const updatedPortDefinitions = new Map(persistedPortDefinitions);
    const referencedPortDefinitions = new Map<string, IRuleBlockPort>();
    const nodeErrors: RuleSaveNodeError[] = [];
    let generatedDisplayOrder = nextGeneratedDisplayOrder(persistedPorts);

    inputPortNodes.forEach((node) => {
        const portId = resolvePortId(node);
        if (!portId) {
            return;
        }
        const persistedPortDefinition = persistedPortDefinitions.get(portId);
        const displayOrder = parseDisplayOrder(node, persistedPortDefinition?.displayOrder ?? generatedDisplayOrder);
        if (displayOrder == null) {
            nodeErrors.push({
                nodeId: node.nodeId,
                message: invalidDisplayOrderMessage,
            });
            return;
        }
        const portDefinition: IRuleBlockPort = {
            id: portId,
            label: ruleEditorNodeParameterValue(node.parameters["label"]) ?? "",
            description: ruleEditorNodeParameterValue(node.parameters["description"]) ?? "",
            exampleValues: ruleEditorNodeParameterValue(node.parameters["exampleValues"]) ?? "",
            displayOrder,
            deprecated: parseDeprecated(node),
        };
        const existingReferencedPortDefinition = referencedPortDefinitions.get(portId);
        if (existingReferencedPortDefinition && !samePortDefinition(existingReferencedPortDefinition, portDefinition)) {
            nodeErrors.push({
                nodeId: node.nodeId,
                message: conflictingPortDefinitionsMessage(portId),
            });
            return;
        }
        if (!existingReferencedPortDefinition) {
            referencedPortDefinitions.set(portId, portDefinition);
            updatedPortDefinitions.set(portId, portDefinition);
        }
        if (!persistedPortDefinition) {
            generatedDisplayOrder = Math.max(generatedDisplayOrder, displayOrder + 1);
        }
    });

    if (nodeErrors.length) {
        return {
            nodeErrors,
        };
    }

    return {
        nodeErrors,
        portDefinitions: sortPortDefinitions(updatedPortDefinitions.values()),
    };
};

/** Validates that logical port display orders remain unique in the merged persisted rule block model. */
const validateDuplicateDisplayOrders = (
    updatedPorts: IRuleBlockPort[],
    inputPortNodes: IRuleOperatorNode[],
    duplicateDisplayOrderMessage: (displayOrder: number) => string,
): RuleSaveNodeError[] => {
    const nodeIdsByPortId = new Map<string, string[]>();
    const nodeErrors: RuleSaveNodeError[] = [];

    inputPortNodes.forEach((node) => {
        const portId = resolvePortId(node);
        if (!portId) {
            return;
        }
        nodeIdsByPortId.set(portId, [...(nodeIdsByPortId.get(portId) ?? []), node.nodeId]);
    });

    duplicateDisplayOrders(updatedPorts).forEach((displayOrder) => {
        updatedPorts.forEach((portDefinition) => {
            if (portDefinition.displayOrder === displayOrder) {
                (nodeIdsByPortId.get(portDefinition.id) ?? []).forEach((nodeId) => {
                    nodeErrors.push({
                        nodeId,
                        message: duplicateDisplayOrderMessage(displayOrder),
                    });
                });
            }
        });
    });

    return nodeErrors;
};

/** Validates that structurally frozen ports keep their ID and display order once the rule block is already in use. */
const validateUsedPortCompatibility = (
    persistedPorts: IRuleBlockPort[],
    updatedPorts: IRuleBlockPort[],
    inputPortNodes: IRuleOperatorNode[],
    removedPortMessage: (portName: string) => string,
    reorderedPortMessage: (portName: string) => string,
): UsedPortCompatibilityResult => {
    const updatedPortsById = new Map(updatedPorts.map((port) => [port.id, port] as const));
    const nodeIdsByPortId = new Map<string, string[]>();
    const removedPortIds: string[] = [];
    const reorderedPortIds = new Set<string>();
    const nodeErrors: RuleSaveNodeError[] = [];

    inputPortNodes.forEach((node) => {
        const portId = resolvePortId(node);
        if (!portId) {
            return;
        }
        nodeIdsByPortId.set(portId, [...(nodeIdsByPortId.get(portId) ?? []), node.nodeId]);
    });

    persistedPorts.forEach((persistedPort) => {
        const updatedPort = updatedPortsById.get(persistedPort.id);
        if (!updatedPort) {
            removedPortIds.push(portDisplayName(persistedPort, persistedPort.id));
        }
    });

    const persistedSharedPortIds = orderedPortIds(
        persistedPorts.filter((port) => updatedPortsById.has(port.id)),
    );
    const updatedSharedPortIds = orderedPortIds(
        updatedPorts.filter((port) => persistedSharedPortIds.includes(port.id)),
    );
    const changedOrderPortIds = persistedSharedPortIds.filter(
        (portId, index) => updatedSharedPortIds[index] !== portId,
    );

    changedOrderPortIds.forEach((portId) => {
        const updatedPort = updatedPortsById.get(portId);
        const portName = portDisplayName(updatedPort, portId);
        reorderedPortIds.add(portName);
        (nodeIdsByPortId.get(portId) ?? []).forEach((nodeId) => {
            nodeErrors.push({
                nodeId,
                message: reorderedPortMessage(portName),
            });
        });
    });

    const errorMessages = [
        ...removedPortIds.map(removedPortMessage),
        ...[...reorderedPortIds].map(reorderedPortMessage),
    ];

    return {
        errorMessage: errorMessages.length > 0 ? errorMessages.join(" ") : undefined,
        nodeErrors,
    };
};

/** Validates whether editing a single logical port would violate the used-rule-block compatibility contract. */
const validateUsedPortUpdateCompatibility = (
    persistedPorts: IRuleBlockPort[],
    currentPorts: IRuleBlockPort[],
    editedPortId: string,
    updatedPort: IRuleBlockPort,
    reorderedPortMessage: (portName: string) => string,
): string | undefined =>
    validateUsedPortCompatibility(
        persistedPorts,
        currentPorts.map((port) => (port.id === editedPortId ? updatedPort : port)),
        [],
        () => "",
        reorderedPortMessage,
    ).errorMessage;

/** Returns the input ports in their stable UI order. */
const sortRuleBlockPorts = (ports: Iterable<IRuleBlockPort>): IRuleBlockPort[] => sortPortDefinitions(ports);

/** Returns true if the current display orders already match the dense canonical rank 1..n. */
const isNormalizedPortDisplayOrder = (ports: IRuleBlockPort[]): boolean =>
    sortPortDefinitions(ports).every((port, index) => port.displayOrder === index + 1);

/** Renumbers display orders to dense ranks 1..n while preserving the current relative order. */
const normalizePortDisplayOrder = (ports: IRuleBlockPort[]): IRuleBlockPort[] =>
    sortPortDefinitions(ports).map((port, index) => ({
        ...port,
        displayOrder: index + 1,
    }));

/** Returns the ports whose visible display order changed between two states. */
const portsWithChangedDisplayOrder = (
    previousPorts: IRuleBlockPort[],
    nextPorts: IRuleBlockPort[],
): IRuleBlockPort[] => {
    const previousPortsById = new Map(previousPorts.map((port) => [port.id, port] as const));
    return nextPorts.filter((port) => {
        const previousPort = previousPortsById.get(port.id);
        return !previousPort || previousPort.displayOrder !== port.displayOrder;
    });
};

/** Generates the next suggested initial values for a newly created input port. */
const nextInputPortDefaults = (
    ports: IRuleBlockPort[],
): Pick<IRuleBlockPort, "label" | "description" | "exampleValues" | "displayOrder" | "deprecated"> => {
    const displayOrder = nextGeneratedDisplayOrder(ports);
    return {
        label: `Input ${displayOrder}`,
        description: "",
        exampleValues: "",
        displayOrder,
        deprecated: false,
    };
};

/** Generates a stable client-side ID for a newly created logical input port. */
const generateInputPortId = (): string =>
    `inputPort_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;

const ruleBlockUtils = {
    assertValidPorts,
    collectPortDefinitions,
    emptyRuleBlockModel,
    generateInputPortId,
    isPersistedPort,
    isNormalizedPortDisplayOrder,
    normalizePortDisplayOrder,
    normalizeStickyNotes,
    portsWithChangedDisplayOrder,
    requirePortId,
    resolvePortId,
    nextInputPortDefaults,
    sortRuleBlockPorts,
    validateMissingPortIds,
    validateDuplicateDisplayOrders,
    validateUsedPortCompatibility,
    validateUsedPortUpdateCompatibility,
};

export default ruleBlockUtils;
