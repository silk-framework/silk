import { StickyNote } from "@eccenca/gui-elements";
import { IRuleOperatorNode, RuleSaveNodeError } from "../../shared/RuleEditor/RuleEditor.typings";
import { ruleEditorNodeParameterValue } from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import { IRuleBlockInputExample, IRuleBlockModel, RuleBlockPort } from "./ruleBlock.types";

/** Creates the default empty rule block model used for new or incomplete tasks. */
const emptyRuleBlockModel = (): IRuleBlockModel => ({
    ports: [],
    inputExamples: [],
    layout: {
        nodePositions: {},
    },
    uiAnnotations: {
        stickyNotes: [],
    },
});

/** Normalizes optional sticky notes to the array shape expected by the editor. */
const normalizeStickyNotes = (stickyNotes?: StickyNote[]): StickyNote[] => stickyNotes ?? [];

/** Deep-clones persisted rule block examples to keep local editor state mutable without mutating task payloads. */
const cloneInputExamples = (inputExamples?: IRuleBlockInputExample[]): IRuleBlockInputExample[] =>
    (inputExamples ?? []).map((example) => ({
        ...example,
        label: typeof example.label === "string" && example.label.trim() ? example.label : undefined,
        inputs: Object.fromEntries(
            Object.entries(example.inputs ?? {}).map(([portId, values]) => [portId, [...values]] as const),
        ),
    }));

/** Drops example inputs for ports that no longer exist in the current logical input-port set. */
const pruneInputExamplesToPorts = (
    inputExamples: IRuleBlockInputExample[],
    ports: Iterable<RuleBlockPort>,
): IRuleBlockInputExample[] => {
    const knownPortIds = new Set([...ports].map((port) => port.id));
    return cloneInputExamples(inputExamples).map((example) => ({
        ...example,
        inputs: Object.fromEntries(Object.entries(example.inputs).filter(([portId]) => knownPortIds.has(portId))),
    }));
};

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

/** Collects the logical input-port IDs that are currently referenced on the rule block canvas. */
const usedInputPortIds = (ruleOperatorNodes: IRuleOperatorNode[]): Set<string> =>
    new Set(
        ruleOperatorNodes
            .filter((node) => node.pluginType === "InputPortOperator")
            .flatMap((node) => {
                const portId = resolvePortId(node);
                return portId ? [portId] : [];
            }),
    );

/** Enforces the logical port invariants expected by the rule block editor runtime. */
const assertValidPorts = (ports: RuleBlockPort[]): void => {
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

const portDisplayName = (port: RuleBlockPort | undefined, fallbackId: string): string =>
    port?.label?.trim() || fallbackId;

const samePortDefinition = (left: RuleBlockPort, right: RuleBlockPort): boolean => {
    return (
        left.id === right.id &&
        left.label === right.label &&
        left.description === right.description &&
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

const nextGeneratedDisplayOrder = (ports: RuleBlockPort[]): number =>
    ports.reduce((maxDisplayOrder, port) => Math.max(maxDisplayOrder, port.displayOrder), 0) + 1;

const sortPortDefinitions = (ports: Iterable<RuleBlockPort>): RuleBlockPort[] =>
    // Valid port definitions keep display orders unique. The secondary ID sort only provides deterministic order for
    // transiently duplicated or malformed states while validation is still running.
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

const orderedPortIds = (ports: Iterable<RuleBlockPort>): string[] => sortPortDefinitions(ports).map((port) => port.id);

const duplicateDisplayOrders = (ports: Iterable<RuleBlockPort>): number[] =>
    [
        ...[...ports]
            .reduce((displayOrderCount, port) => {
                displayOrderCount.set(port.displayOrder, (displayOrderCount.get(port.displayOrder) ?? 0) + 1);
                return displayOrderCount;
            }, new Map<number, number>())
            .entries(),
    ]
        .filter(([, count]) => count > 1)
        .map(([displayOrder]) => displayOrder)
        .sort((left, right) => left - right);

interface PortDefinitionCollectionResult {
    nodeErrors: RuleSaveNodeError[];
    portDefinitions?: RuleBlockPort[];
}

interface UsedPortCompatibilityResult {
    errorMessage?: string;
    nodeErrors: RuleSaveNodeError[];
}

/** Returns true if a logical port belongs to the persisted rule block baseline. */
const isPersistedPort = (persistedPorts: Iterable<RuleBlockPort>, portId: string): boolean =>
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
    persistedPorts: RuleBlockPort[],
    inputPortNodes: IRuleOperatorNode[],
    invalidDisplayOrderMessage: string,
    conflictingPortDefinitionsMessage: (portId: string) => string,
): PortDefinitionCollectionResult => {
    const persistedPortDefinitions = new Map(persistedPorts.map((port) => [port.id, port] as const));
    const updatedPortDefinitions = new Map(persistedPortDefinitions);
    const referencedPortDefinitions = new Map<string, RuleBlockPort>();
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
        const portDefinition: RuleBlockPort = {
            id: portId,
            label: ruleEditorNodeParameterValue(node.parameters["label"]) ?? "",
            description: ruleEditorNodeParameterValue(node.parameters["description"]) ?? "",
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
    updatedPorts: RuleBlockPort[],
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

/** Returns true if the logical rule block ports currently contain duplicate display orders. */
const hasDuplicateDisplayOrders = (ports: Iterable<RuleBlockPort>): boolean => duplicateDisplayOrders(ports).length > 0;

/** Validates that structurally frozen ports keep their ID and display order once the rule block is already in use. */
const validateUsedPortCompatibility = (
    persistedPorts: RuleBlockPort[],
    updatedPorts: RuleBlockPort[],
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

    const persistedSharedPortIds = orderedPortIds(persistedPorts.filter((port) => updatedPortsById.has(port.id)));
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
    persistedPorts: RuleBlockPort[],
    currentPorts: RuleBlockPort[],
    editedPortId: string,
    updatedPort: RuleBlockPort,
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
const sortRuleBlockPorts = (ports: Iterable<RuleBlockPort>): RuleBlockPort[] => sortPortDefinitions(ports);

/** Returns true if the current display orders already match the dense canonical rank 1..n. */
const isNormalizedPortDisplayOrder = (ports: RuleBlockPort[]): boolean =>
    sortPortDefinitions(ports).every((port, index) => port.displayOrder === index + 1);

/** Returns logical ports that currently exist in the rule block but are not referenced by any canvas node. */
const unusedRuleBlockPorts = (
    ports: Iterable<RuleBlockPort>,
    usedPortIds: Set<string>,
    includeDeprecated: boolean = true,
): RuleBlockPort[] =>
    sortPortDefinitions(ports).filter((port) => !usedPortIds.has(port.id) && (includeDeprecated || !port.deprecated));

/** Renumbers display orders to dense ranks 1..n while preserving the current relative order. */
const normalizePortDisplayOrder = (ports: RuleBlockPort[]): RuleBlockPort[] =>
    sortPortDefinitions(ports).map((port, index) => ({
        ...port,
        displayOrder: index + 1,
    }));

/** Returns the ports whose visible display order changed between two states. */
const portsWithChangedDisplayOrder = (previousPorts: RuleBlockPort[], nextPorts: RuleBlockPort[]): RuleBlockPort[] => {
    const previousPortsById = new Map(previousPorts.map((port) => [port.id, port] as const));
    return nextPorts.filter((port) => {
        const previousPort = previousPortsById.get(port.id);
        return !previousPort || previousPort.displayOrder !== port.displayOrder;
    });
};

/** Generates the next suggested initial values for a newly created input port. */
const nextInputPortDefaults = (
    ports: RuleBlockPort[],
): Pick<RuleBlockPort, "label" | "description" | "displayOrder" | "deprecated"> => {
    const displayOrder = nextGeneratedDisplayOrder(ports);
    return {
        label: `Input ${displayOrder}`,
        description: "",
        displayOrder,
        deprecated: false,
    };
};

/** Generates a stable client-side ID for a newly created logical input port. */
const generateInputPortId = (): string =>
    `inputPort_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;

/** Generates a stable client-side ID for a newly created rule block input example. */
const generateInputExampleId = (): string =>
    `example_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;

const ruleBlockUtils = {
    assertValidPorts,
    cloneInputExamples,
    collectPortDefinitions,
    emptyRuleBlockModel,
    generateInputExampleId,
    generateInputPortId,
    isPersistedPort,
    isNormalizedPortDisplayOrder,
    normalizePortDisplayOrder,
    normalizeStickyNotes,
    portsWithChangedDisplayOrder,
    pruneInputExamplesToPorts,
    requirePortId,
    resolvePortId,
    nextInputPortDefaults,
    sortRuleBlockPorts,
    unusedRuleBlockPorts,
    usedInputPortIds,
    hasDuplicateDisplayOrders,
    validateMissingPortIds,
    validateDuplicateDisplayOrders,
    validateUsedPortCompatibility,
    validateUsedPortUpdateCompatibility,
};

export default ruleBlockUtils;
