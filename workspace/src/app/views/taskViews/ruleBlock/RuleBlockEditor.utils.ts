import React from "react";
import { IconButton, StickyNote } from "@eccenca/gui-elements";
import { IProjectTask } from "@ducks/shared/typings";
import i18next from "i18next";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleEditorPatchableNodeProjection,
} from "../../shared/RuleEditor/RuleEditor.typings";
import ruleUtils from "../shared/rules/rule.utils";
import { IRuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";
import { IPreConfiguredRuleOperator } from "../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;
type InputPortListItem = IRuleBlockPort | { type: "create" };

/** Creates the pseudo-operator used to edit rule block input ports inside the shared rule editor. */
const createInputPortOperator = (): IRuleOperator => {
    return {
        pluginType: "InputPortOperator",
        pluginId: "inputPort",
        label: i18next.t("taskViews.ruleBlock.inputPortOperator"),
        description: i18next.t("taskViews.ruleBlock.inputPortOperatorDescription"),
        parameterSpecification: {},
        portSpecification: {
            type: "count",
            minInputPorts: 0,
            maxInputPorts: 0,
        },
        categories: [i18next.t("taskViews.ruleBlock.inputPortsTab")],
        tags: [],
        inputsCanBeSwitched: false,
    };
};

/** Creates the synthetic sidebar entry used to start input-port creation. */
const createNewInputPortSidebarItem = (onCreate: () => void): IPreConfiguredRuleOperator => {
    const inputPortOperator = createInputPortOperator();
    return {
        pluginType: "InputPortOperator",
        pluginId: "newInputPort",
        label: i18next.t("taskViews.ruleBlock.newInputPort"),
        description: inputPortOperator.description,
        icon: "item-add-artefact",
        categories: inputPortOperator.categories,
        tags: [],
        inputsCanBeSwitched: false,
        parameterOverwrites: {},
        draggable: false,
        actions: React.createElement(IconButton, {
            key: "create-input-port",
            name: "item-add-artefact",
            onClick: onCreate,
            size: "small",
            intent: "accent"
        }),
    };
};

/** Converts one logical input port into a draggable pre-configured sidebar operator. */
const convertInputPortToSidebarOperator = (
    port: IRuleBlockPort,
    onEdit: (portId: string) => void,
    onDelete: (portId: string) => void,
): IPreConfiguredRuleOperator => {
    const inputPortOperator = createInputPortOperator();
    return {
        pluginType: inputPortOperator.pluginType,
        pluginId: inputPortOperator.pluginId,
        label: port.label,
        description: port.description,
        icon: inputPortOperator.icon,
        categories: [],
        tags: [
            i18next.t("taskViews.ruleBlock.inputPortsTab"),
            String(port.displayOrder),
            ...(port.deprecated ? [i18next.t("taskViews.ruleBlock.deprecated")] : []),
        ],
        inputsCanBeSwitched: inputPortOperator.inputsCanBeSwitched,
        markdownDocumentation: inputPortOperator.markdownDocumentation,
        actions: [
            React.createElement(IconButton, {
                key: `edit-input-port-${port.id}`,
                name: "item-edit",
                onClick: () => onEdit(port.id),
                size: "small",
            }),
            React.createElement(IconButton, {
                key: `delete-input-port-${port.id}`,
                name: "item-remove",
                onClick: () => onDelete(port.id),
                size: "small",
                intent: "danger"
            }),
        ],
        parameterOverwrites: {
            portId: port.id,
        },
        nodeMetaDataOverwrites: inputPortNodeMetaData(port),
    };
};

/** Creates the input-port pre-configured sidebar tab from the current logical input-port state. */
const createInputPortsTab = (
    ports: IRuleBlockPort[],
    onCreate: () => void,
    onEdit: (portId: string) => void,
    onDelete: (portId: string) => void,
): IRuleSidebarPreConfiguredOperatorsTabConfig<InputPortListItem> => ({
    id: "inputPorts",
    icon: "data-sourcepath",
    label: i18next.t("taskViews.ruleBlock.inputPortsTab"),
    position: "top",
    defaultOperators: [],
    fetchOperators: async () => [{ type: "create" }, ...ruleBlockUtils.sortRuleBlockPorts(ports)],
    convertToOperator: (listItem) =>
        "type" in listItem
            ? createNewInputPortSidebarItem(onCreate)
            : convertInputPortToSidebarOperator(listItem, onEdit, onDelete),
    isOriginalOperator: (item): item is InputPortListItem => "type" in (item as InputPortListItem) || "id" in item,
    itemSearchText: (listItem) =>
        "type" in listItem
            ? i18next.t("taskViews.ruleBlock.newInputPort").toLowerCase()
            : `${listItem.label} ${listItem.description ?? ""} ${listItem.displayOrder}`.toLowerCase(),
    itemLabel: (listItem) =>
        "type" in listItem ? i18next.t("taskViews.ruleBlock.newInputPort") : listItem.label,
    itemId: (listItem) => ("type" in listItem ? "newInputPort" : listItem.id),
});

/** Enriches an input port node with persisted port metadata from the current rule block model. */
const inputPortNodeMetaData = (portDefinition: IRuleBlockPort | undefined): RuleEditorPatchableNodeProjection => ({
    label: portDefinition?.label || i18next.t("taskViews.ruleBlock.inputPortOperator"),
    description: portDefinition?.description ?? "",
    tags: [
        i18next.t("taskViews.ruleBlock.inputPortsTab"),
        ...(portDefinition?.displayOrder != null ? [String(portDefinition.displayOrder)] : []),
        ...(portDefinition?.deprecated ? [i18next.t("taskViews.ruleBlock.deprecated")] : []),
    ],
});

/** Enriches an input port node with persisted port metadata from the current rule block model. */
const updateInputPortNode = (
    node: IRuleOperatorNode,
    portDefinitions: Map<string, IRuleBlockPort>,
): void => {
    const portId = ruleBlockUtils.resolvePortId(node);
    const portDefinition = portDefinitions.get(portId);
    const metaData = inputPortNodeMetaData(portDefinition);
    node.label = metaData.label;
    node.description = metaData.description;
    node.tags = metaData.tags;
    node.parameters = {
        portId,
    };
};

/** Converts a persisted rule block task into the rule editor node list used by the canvas. */
const convertRuleBlockTaskToRuleOperatorNodes = (
    ruleBlockTask: RuleBlockTaskData,
    ruleOperator: RuleOperatorFetchFnType,
): IRuleOperatorNode[] => {
    const operatorTree = ruleBlockTask.data.parameters.ruleBlockModel?.operatorTree;
    if (!operatorTree) {
        return [];
    }
    const portDefinitions = new Map(
        (ruleBlockTask.data.parameters.ruleBlockModel?.ports ?? []).map((port) => [port.id, port] as const),
    );
    const operatorNodes: IRuleOperatorNode[] = [];
    ruleUtils.extractOperatorNodeFromValueInput(operatorTree, operatorNodes, undefined, ruleOperator);
    operatorNodes.forEach((node) => {
        if (node.pluginType === "InputPortOperator") {
            updateInputPortNode(node, portDefinitions);
        }
    });
    const nodePositions = ruleBlockTask.data.parameters.ruleBlockModel?.layout?.nodePositions ?? {};
    operatorNodes.forEach((node) => {
        const position = nodePositions[node.nodeId];
        if (position) {
            node.position = {
                x: position.x,
                y: position.y,
            };
            node.dimension = {
                ...node.dimension,
                width: position.width ?? undefined,
                height: position.height ?? undefined,
            };
        }
    });
    return operatorNodes;
};

/** Returns the sticky notes stored on the current rule block task. */
const getStickyNotes = (ruleBlockTask: RuleBlockTaskData | undefined): StickyNote[] =>
    ruleBlockUtils.normalizeStickyNotes(ruleBlockTask?.data.parameters.ruleBlockModel?.uiAnnotations?.stickyNotes);

const ruleBlockEditorUtils = {
    convertInputPortToSidebarOperator,
    convertRuleBlockTaskToRuleOperatorNodes,
    createInputPortsTab,
    createInputPortOperator,
    getStickyNotes,
    inputPortNodeMetaData,
};

export default ruleBlockEditorUtils;
