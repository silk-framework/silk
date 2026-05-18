import { StickyNote } from "@eccenca/gui-elements";
import { IProjectTask } from "@ducks/shared/typings";
import i18next from "i18next";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import { IRuleOperator, IRuleOperatorNode } from "../../shared/RuleEditor/RuleEditor.typings";
import ruleUtils from "../shared/rules/rule.utils";
import { IRuleBlockPort, IRuleBlockTaskParameters } from "./ruleBlock.types";
import ruleBlockUtils from "./ruleBlock.utils";

type RuleBlockTaskData = IProjectTask<IRuleBlockTaskParameters>;

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
    convertRuleBlockTaskToRuleOperatorNodes,
    createInputPortOperator,
    getStickyNotes,
};

export default ruleBlockEditorUtils;
