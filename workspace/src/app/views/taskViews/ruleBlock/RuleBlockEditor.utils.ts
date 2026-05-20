import React from "react";
import { IconButton, StickyNote } from "@eccenca/gui-elements";
import { IProjectTask } from "@ducks/shared/typings";
import i18next from "i18next";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
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
        parameterSpecification: {
            label: ruleUtils.parameterSpecification({
                label: i18next.t("form.field.label"),
                required: false,
                orderIdx: 0,
            }),
            description: ruleUtils.parameterSpecification({
                label: i18next.t("common.words.description"),
                required: false,
                type: "textArea",
                orderIdx: 1,
            }),
            exampleValues: ruleUtils.parameterSpecification({
                label: i18next.t("taskViews.ruleBlock.exampleValues"),
                required: false,
                type: "code-yaml",
                orderIdx: 2,
            }),
            displayOrder: ruleUtils.parameterSpecification({
                label: i18next.t("taskViews.ruleBlock.displayOrder"),
                required: false,
                type: "int",
                orderIdx: 3,
            }),
            deprecated: ruleUtils.parameterSpecification({
                label: i18next.t("taskViews.ruleBlock.deprecated"),
                required: false,
                type: "boolean",
                defaultValue: "false",
                orderIdx: 4,
            }),
        },
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
): IPreConfiguredRuleOperator => {
    const inputPortOperator = createInputPortOperator();
    return {
        pluginType: inputPortOperator.pluginType,
        pluginId: inputPortOperator.pluginId,
        label: port.label,
        description: inputPortOperator.description,
        icon: inputPortOperator.icon,
        categories: [],
        tags: [i18next.t("taskViews.ruleBlock.inputPortsTab"), String(port.displayOrder)],
        inputsCanBeSwitched: inputPortOperator.inputsCanBeSwitched,
        markdownDocumentation: inputPortOperator.markdownDocumentation,
        actions: React.createElement(IconButton, {
            key: `edit-input-port-${port.id}`,
            name: "item-edit",
            onClick: () => onEdit(port.id),
            size: "small",
        }),
        parameterOverwrites: {
            portId: port.id,
            label: port.label,
            description: port.description,
            exampleValues: port.exampleValues,
            displayOrder: String(port.displayOrder),
            deprecated: port.deprecated ? "true" : "false",
        },
    };
};

/** Creates the input-port pre-configured sidebar tab from the current logical input-port state. */
const createInputPortsTab = (
    ports: IRuleBlockPort[],
    onCreate: () => void,
    onEdit: (portId: string) => void,
): IRuleSidebarPreConfiguredOperatorsTabConfig<InputPortListItem> => ({
    id: "inputPorts",
    icon: "data-sourcepath",
    label: i18next.t("taskViews.ruleBlock.inputPortsTab"),
    position: "top",
    defaultOperators: [],
    fetchOperators: async () => [{ type: "create" }, ...ruleBlockUtils.sortRuleBlockPorts(ports)],
    convertToOperator: (listItem) =>
        "type" in listItem ? createNewInputPortSidebarItem(onCreate) : convertInputPortToSidebarOperator(listItem, onEdit),
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
const updateInputPortNode = (
    node: IRuleOperatorNode,
    portDefinitions: Map<string, IRuleBlockPort>,
): void => {
    const portId = ruleBlockUtils.resolvePortId(node);
    const portDefinition = portDefinitions.get(portId);
    node.label = portDefinition?.label || i18next.t("taskViews.ruleBlock.inputPortOperator");
    node.description = i18next.t("taskViews.ruleBlock.inputPortOperatorDescription");
    node.parameters = {
        ...node.parameters,
        label: portDefinition?.label ?? "",
        description: portDefinition?.description ?? "",
        exampleValues: portDefinition?.exampleValues ?? "",
        displayOrder: portDefinition?.displayOrder != null ? String(portDefinition.displayOrder) : "",
        deprecated: portDefinition?.deprecated ? "true" : "false",
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
};

export default ruleBlockEditorUtils;
