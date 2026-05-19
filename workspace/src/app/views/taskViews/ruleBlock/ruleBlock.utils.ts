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
const resolvePortId = (node: IRuleOperatorNode): string =>
    (ruleEditorNodeParameterValue(node.parameters["portId"]) ?? node.nodeId).trim() || node.nodeId;

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
    [...ports].sort((left, right) => left.displayOrder - right.displayOrder || left.id.localeCompare(right.id));

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
    const reorderedPortIds: string[] = [];
    const nodeErrors: RuleSaveNodeError[] = [];

    inputPortNodes.forEach((node) => {
        const portId = resolvePortId(node);
        nodeIdsByPortId.set(portId, [...(nodeIdsByPortId.get(portId) ?? []), node.nodeId]);
    });

    persistedPorts.forEach((persistedPort) => {
        const updatedPort = updatedPortsById.get(persistedPort.id);
        if (!updatedPort) {
            removedPortIds.push(portDisplayName(persistedPort, persistedPort.id));
        } else if (updatedPort.displayOrder !== persistedPort.displayOrder) {
            const portName = portDisplayName(updatedPort, persistedPort.id);
            reorderedPortIds.push(portName);
            (nodeIdsByPortId.get(persistedPort.id) ?? []).forEach((nodeId) => {
                nodeErrors.push({
                    nodeId,
                    message: reorderedPortMessage(portName),
                });
            });
        }
    });

    const errorMessages = [
        ...removedPortIds.map(removedPortMessage),
        ...reorderedPortIds.map(reorderedPortMessage),
    ];

    return {
        errorMessage: errorMessages.length > 0 ? errorMessages.join(" ") : undefined,
        nodeErrors,
    };
};

const ruleBlockUtils = {
    collectPortDefinitions,
    emptyRuleBlockModel,
    normalizeStickyNotes,
    resolvePortId,
    validateDuplicateDisplayOrders,
    validateUsedPortCompatibility,
};

export default ruleBlockUtils;
