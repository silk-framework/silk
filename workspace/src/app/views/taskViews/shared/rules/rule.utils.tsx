import { IPathInput, ITransformOperator, IValueInput, RuleLayout } from "./rule.typings";
import {
    IParameterSpecification,
    IParameterValidationResult,
    IPortSpecification,
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSideBarFilterTabConfig,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleEditorValidationNode,
    RuleParameterType,
    RuleValidationError,
} from "../../../shared/RuleEditor/RuleEditor.typings";
import { RuleOperatorFetchFnType } from "../../../shared/RuleEditor/RuleEditor";
import { IPluginDetails } from "@ducks/common/typings";
import {
    RuleEditorNodeParameterValue,
    ruleEditorNodeParameterValue,
} from "../../../shared/RuleEditor/model/RuleEditorModel.typings";
import { IAutocompleteDefaultResponse } from "@ducks/shared/typings";
import React from "react";
import {
    Highlighter,
    OverflowText,
    OverviewItem,
    OverviewItemDescription,
    OverviewItemLine,
} from "@eccenca/gui-elements";
import { IRenderModifiers } from "@eccenca/gui-elements/src/components/AutocompleteField/AutoCompleteField";
import { CLASSPREFIX as eccguiprefix } from "@eccenca/gui-elements/src/configuration/constants";
import { optionallyLabelledParameterToValue } from "../../linking/linking.types";

/** Extracts the operator node from a path input. */
const extractOperatorNodeFromPathInput = (
    pathInput: IPathInput,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined
): string => {
    result.push({
        nodeId: pathInput.id,
        label: isTarget ? "Target path" : "Source path",
        pluginType: "PathInputOperator",
        pluginId: isTarget ? "targetPathInput" : "sourcePathInput", // We use the plugin ID to denote if this is a source or target path input.
        inputs: [],
        parameters: {
            path: pathInput.path,
        },
        portSpecification: {
            minInputPorts: 0,
            maxInputPorts: 0,
        },
        description: "Specifies the property, attribute or path where the input values are coming from.",
        inputsCanBeSwitched: false,
    });
    return pathInput.id;
};

/** Extract the operator node from a transform input. */
const extractOperatorNodeFromTransformInput = (
    transformInput: ITransformOperator,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined,
    ruleOperator: RuleOperatorFetchFnType
): string => {
    const inputs = transformInput.inputs.map((input) =>
        extractOperatorNodeFromValueInput(input, result, isTarget, ruleOperator)
    );
    const op = ruleOperator(transformInput.function, "TransformOperator");
    result.push({
        nodeId: transformInput.id,
        inputs: inputs,
        pluginType: "TransformOperator",
        pluginId: transformInput.function,
        label: op?.label ?? transformInput.function,
        parameters: transformInput.parameters,
        portSpecification: {
            minInputPorts: 1,
        },
        tags: ["Transform"],
        description: op?.description,
        inputsCanBeSwitched: false,
        markdownDocumentation: op?.markdownDocumentation,
    });
    return transformInput.id;
};

/** Extract operator nodes from an value input node, i.e. path input or transform operator.
 *
 * @param operator The value input operator.
 * @param result   The result array this operator should be added to.
 * @param isTarget Only important in the context of comparisons where we have to distinguish between source and target paths.
 */
const extractOperatorNodeFromValueInput = (
    operator: IValueInput | undefined,
    result: IRuleOperatorNode[],
    isTarget: boolean | undefined,
    ruleOperator: (pluginId: string, pluginType?: string) => IRuleOperator | undefined
): string | undefined => {
    if (operator) {
        const nodeId =
            operator.type === "pathInput"
                ? extractOperatorNodeFromPathInput(operator as IPathInput, result, isTarget)
                : extractOperatorNodeFromTransformInput(operator as ITransformOperator, result, isTarget, ruleOperator);
        return nodeId;
    }
};

const customInputPathRenderer = (
    autoCompleteResponse: IAutocompleteDefaultResponse,
    query: string,
    modifiers: IRenderModifiers,
    handleSelectClick: () => any
): JSX.Element | string => {
    return autoCompleteResponse.label ? (
        <OverviewItem
            key={autoCompleteResponse.value}
            onClick={handleSelectClick}
            hasSpacing={true}
            className={modifiers.active ? `${eccguiprefix}-overviewitem__item--active` : ""}
        >
            <OverviewItemDescription style={{ maxWidth: "50vw" }}>
                <OverviewItemLine>
                    <OverflowText inline={true} style={{ width: "100vw" }}>
                        <Highlighter label={autoCompleteResponse.label} searchValue={query} />
                    </OverflowText>
                </OverviewItemLine>
                <OverviewItemLine small={true}>
                    <OverflowText inline={true} style={{ width: "100vw" }}>
                        <Highlighter label={autoCompleteResponse.value} searchValue={query} />
                    </OverflowText>
                </OverviewItemLine>
            </OverviewItemDescription>
        </OverviewItem>
    ) : (
        autoCompleteResponse.value
    );
};

/** Input path operator used in the transform and linking operators. */
const inputPathOperator = (
    pluginId: string,
    label: string,
    additionalCategories: string[],
    description?: string,
    customAutoCompletionRequest?: (
        textQuery: string,
        limit: number
    ) => IAutocompleteDefaultResponse[] | Promise<IAutocompleteDefaultResponse[]>,
    customValidation?: (value: RuleEditorNodeParameterValue) => IParameterValidationResult
): IRuleOperator => {
    return {
        pluginType: "PathInputOperator",
        pluginId: pluginId,
        portSpecification: {
            minInputPorts: 0,
            maxInputPorts: 0,
        },
        label: label,
        parameterSpecification: {
            path: parameterSpecification({
                label: "Path",
                type: "pathInput",
                description: "The source input path as Silk path expression.",
                defaultValue: "",
                customValidation,
                autoCompletion: customAutoCompletionRequest
                    ? {
                          allowOnlyAutoCompletedValues: false,
                          autoCompleteValueWithLabels: true,
                          autoCompletionDependsOnParameters: [],
                          customAutoCompletionRequest,
                          customItemRenderer: customInputPathRenderer,
                      }
                    : undefined,
            }),
        },
        categories: [...additionalCategories, "Recommended"],
        icon: undefined,
        description: description,
        tags: [],
        inputsCanBeSwitched: false,
    };
};

type OptionalParameterAttributes = "defaultValue" | "type" | "advanced" | "required";
/** Parameter specification convenience function. */
const parameterSpecification = ({
    label,
    defaultValue = "",
    type = "textField",
    description,
    advanced = false,
    required = true,
    customValidation,
    autoCompletion,
}: Omit<IParameterSpecification, OptionalParameterAttributes> &
    Partial<Pick<IParameterSpecification, OptionalParameterAttributes>>): IParameterSpecification => {
    return {
        label,
        defaultValue,
        type,
        description,
        advanced,
        required,
        customValidation,
        autoCompletion,
    };
};

const REVERSE_PARAMETER_ID = "reverse";

/** Converts plugin details from the backend to rule operators.
 *
 * @param pluginDetails                      The details of the operator plugin.
 * @param addAdditionParameterSpecifications Callback function to decide if additional parameters should be added to an operator.
 */
const convertRuleOperator = (
    pluginDetails: IPluginDetails,
    addAdditionParameterSpecifications: (pluginDetails: IPluginDetails) => [id: string, spec: IParameterSpecification][]
): IRuleOperator => {
    const required = (parameterId: string) => pluginDetails.required.includes(parameterId);
    // If this is true then source and target inputs can be connected in any order.
    const inputsCanBeSwitched =
        pluginDetails.properties[REVERSE_PARAMETER_ID]?.parameterType === "boolean" &&
        pluginDetails.pluginType === "ComparisonOperator";
    const additionalParamSpecs = addAdditionParameterSpecifications(pluginDetails);
    return {
        pluginType: pluginDetails.pluginType ?? "unknown",
        pluginId: pluginDetails.pluginId,
        label: pluginDetails.title,
        description: pluginDetails.description,
        categories: pluginDetails.categories,
        icon: undefined,
        markdownDocumentation: pluginDetails.markdownDocumentation,
        parameterSpecification: Object.fromEntries([
            ...Object.entries(pluginDetails.properties)
                .filter(([paramId, paramSpec]) => !inputsCanBeSwitched || paramId !== REVERSE_PARAMETER_ID)
                .map(([parameterId, parameterSpec]) => {
                    const spec: IParameterSpecification = {
                        label: parameterSpec.title,
                        description: parameterSpec.description,
                        advanced: parameterSpec.advanced,
                        required: required(parameterId),
                        type: convertPluginParameterType(parameterSpec.parameterType),
                        autoCompletion: parameterSpec.autoCompletion,
                        defaultValue: optionallyLabelledParameterToValue(parameterSpec.value) ?? "",
                    };
                    return [parameterId, spec];
                }),
            ...additionalParamSpecs,
        ]),
        portSpecification: portSpecification(pluginDetails),
        tags: pluginTags(pluginDetails),
        inputsCanBeSwitched,
    };
};

// Converts the parameter type of the plugin to any of the supported types of the parameter UI component
const convertPluginParameterType = (pluginParameterType: string): RuleParameterType => {
    switch (pluginParameterType) {
        case "multiline string":
            return "textArea";
        case "int":
        case "Long":
        case "option[int]":
            return "int";
        case "boolean":
            return "boolean";
        case "stringmap": // FIXME: Investigate how common this type is
            return "textArea";
        case "double":
            return "float";
        case "traversable[string]": // FIXME: Have some kind of list component here?
            return "textArea";
        case "restriction":
            return "code";
        case "password":
            return "password";
        case "resource":
            return "resource";
        case "duration":
        case "char": // FIXME: We could further restrict its target type
        case "uri": // FIXME: We could handle URIs with a special target type
        case "option[identifier]": // FIXME: We could check identifiers
        case "identifier":
        case "enumeration":
        case "project":
        case "task":
        default:
            return "textField";
    }
};

/** Tags for a rule operator based on its plugin specification. */
const pluginTags = (pluginDetails: IPluginDetails): string[] => {
    switch (pluginDetails.pluginType) {
        case "TransformOperator":
            return ["Transform"];
        case "ComparisonOperator":
            return ["Comparison"];
        case "AggregationOperator":
            return ["Aggregation"];
        default:
            return [];
    }
};

const portSpecification = (op: IPluginDetails): IPortSpecification => {
    switch (op.pluginType) {
        case "ComparisonOperator":
            return { minInputPorts: 2, maxInputPorts: 2 };
        default:
            return { minInputPorts: 1 };
    }
};

/** Converts a rule operator node to a value input. */
const convertRuleOperatorNodeToValueInput = (
    ruleOperatorNode: IRuleOperatorNode,
    ruleOperatorNodes: Map<string, IRuleOperatorNode>
): IValueInput => {
    if (ruleOperatorNode.pluginType === "TransformOperator") {
        const transformOperator: ITransformOperator = {
            id: ruleOperatorNode.nodeId,
            function: ruleOperatorNode.pluginId,
            inputs: ruleOperatorNode.inputs
                .filter((i) => i != null)
                .map((i) =>
                    convertRuleOperatorNodeToValueInput(
                        fetchRuleOperatorNode(i!!, ruleOperatorNodes, ruleOperatorNode),
                        ruleOperatorNodes
                    )
                ),
            parameters: Object.fromEntries(
                Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                    parameterKey,
                    parameterValue ?? "",
                ])
            ),
            type: "transformInput",
        };
        return transformOperator;
    } else if (ruleOperatorNode.pluginType === "PathInputOperator") {
        const pathInput: IPathInput = {
            id: ruleOperatorNode.nodeId,
            path: ruleEditorNodeParameterValue(ruleOperatorNode.parameters["path"]) ?? "",
            type: "pathInput",
        };
        return pathInput;
    } else {
        throw Error(
            `Tried to convert ${ruleOperatorNode.pluginType} node '${ruleOperatorNode.label}' to incompatible value input!`
        );
    }
};

/** Fetches and operator node from the available nodes. */
const fetchRuleOperatorNode = (
    nodeId: string,
    ruleOperators: Map<string, IRuleOperatorNode>,
    parentNode?: IRuleOperatorNode
): IRuleOperatorNode => {
    const ruleOperatorNode = ruleOperators.get(nodeId);
    if (ruleOperatorNode) {
        return ruleOperatorNode;
    } else {
        throw new Error(
            `Rule operator node with ID '${nodeId}' does not exist${
                parentNode ? `, but is defined as input for node '${parentNode.label}'!` : "!"
            }`
        );
    }
};

/** Converts the editor rule operator nodes to a map from ID to node and also returns all root nodes, i.e. nodes without parent. */
const convertToRuleOperatorNodeMap = (
    ruleOperatorNodes: IRuleOperatorNode[],
    validate: boolean
): [Map<string, IRuleOperatorNode>, IRuleOperatorNode[]] => {
    const hasParent = new Set<string>();
    const nodeMap = new Map<string, IRuleOperatorNode>(
        ruleOperatorNodes.map((node) => {
            node.inputs.filter((i) => i != null).forEach((i) => hasParent.add(i!!));
            return [node.nodeId, node];
        })
    );
    const rootNodes = ruleOperatorNodes.filter((node) => !hasParent.has(node.nodeId));
    if (validate && rootNodes.length > 1) {
        throw new RuleValidationError(
            `More than one root node found, but at most one is allowed! Root nodes: ${rootNodes
                .map((n) => n.label)
                .join(", ")}`,
            rootNodes.map((node) => ({
                nodeId: node.nodeId,
                message: `Rule operator '${node.label}' is not the only root node.`,
            }))
        );
    } else if (validate && rootNodes.length === 0 && nodeMap.size > 0) {
        throw Error(`Rule tree cannot be saved, because it contains cycles!`);
    } else if (validate && rootNodes.length === 1) {
        const cycle = findCycles(rootNodes[0], nodeMap);
        if (cycle) {
            throw new RuleValidationError(
                "Illegal cycle found in rule. Path from root node to cycled node: " +
                    cycle.map((n) => n.label).join(", "),
                cycle
            );
        }
    }
    return [nodeMap, rootNodes];
};

/** Returns the first cycle found if any exist. */
const findCycles = (
    rootNode: IRuleOperatorNode,
    nodeMap: Map<string, IRuleOperatorNode>
): IRuleOperatorNode[] | undefined => {
    const visitedNodes = new Set<string>();
    const iterate = (operatorNode: IRuleOperatorNode): IRuleOperatorNode[] | undefined => {
        if (visitedNodes.has(operatorNode.nodeId)) {
            return [operatorNode];
        } else {
            visitedNodes.add(operatorNode.nodeId);
            operatorNode.inputs.forEach((child) => {
                if (child && nodeMap.has(child)) {
                    const result = iterate(nodeMap.get(child)!!);
                    if (result) {
                        result.push(operatorNode);
                        return result;
                    }
                }
            });
        }
    };
    const result = iterate(rootNode);
    if (visitedNodes.size !== nodeMap.size) {
        throw new RuleValidationError(
            `Root node '${rootNode.label}' is not connected to all nodes! There are overall ${nodeMap.size} nodes, but only ${visitedNodes.size} are part of the rule tree spanned by '${rootNode.label}'.`,
            [rootNode]
        );
    }
    return result ? result.reverse() : undefined;
};

/** Extract rule layout from rule operator nodes. */
const ruleLayout = (nodes: IRuleOperatorNode[]): RuleLayout => {
    const nodePositions: { [key: string]: [number, number] } = Object.create(null);
    nodes.forEach((node) => {
        if (node.position) {
            nodePositions[node.nodeId] = [Math.round(node.position.x), Math.round(node.position.y)];
        }
    });
    return {
        nodePositions,
    };
};

/** Specifies the allowed connections. Only connections that return true are allowed. */
const validateConnection = (
    fromRuleOperatorNode: RuleEditorValidationNode,
    toRuleOperatorNode: RuleEditorValidationNode,
    targetPortIdx: number
): boolean => {
    const sourcePluginType = fromRuleOperatorNode.node.pluginType;
    const sourcePluginId = fromRuleOperatorNode.node.pluginId;
    const targetPluginType = toRuleOperatorNode.node.pluginType;
    switch (sourcePluginType) {
        case "PathInputOperator":
            // Target must be either a comparison or a transform operator
            if (targetPluginType === "ComparisonOperator") {
                if (toRuleOperatorNode.node.inputsCanBeSwitched) {
                    // Check that the other input is different or missing
                    const otherPort = targetPortIdx === 0 ? 1 : 0;
                    const inputs = toRuleOperatorNode.inputs();
                    const otherInput = inputs[otherPort];
                    const sourceInputType = sourcePluginId === "sourcePathInput" ? "source" : "target";
                    return otherInput == null || fromType(otherInput) !== sourceInputType;
                } else {
                    return (
                        (sourcePluginId === "targetPathInput" && targetPortIdx === 1) ||
                        (sourcePluginId === "sourcePathInput" && targetPortIdx === 0)
                    );
                }
            } else {
                return (
                    targetPluginType === "TransformOperator" &&
                    inputPathValidation(fromRuleOperatorNode, toRuleOperatorNode, targetPortIdx)
                );
            }
        case "ComparisonOperator":
            return targetPluginType === "AggregationOperator";
        case "AggregationOperator":
            return targetPluginType === "AggregationOperator";
        case "TransformOperator":
            return (
                (targetPluginType === "ComparisonOperator" || targetPluginType === "TransformOperator") &&
                inputPathValidation(fromRuleOperatorNode, toRuleOperatorNode, targetPortIdx)
            );
        default:
            return true;
    }
};

/** A node can be a source value node, target value node (source/target paths, transformations) or none (comparison, aggregation). */
type PathValidationType = "source" | "target" | undefined;

/** Finds out from where the values come, either "source", "target" or undefined (meaning not possible to determine) */
const fromType = (node: RuleEditorValidationNode, filterInputIdx?: number): PathValidationType => {
    switch (node.node.pluginType) {
        case "PathInputOperator":
            return node.node.pluginId === "sourcePathInput" ? "source" : "target";
        case "TransformOperator":
            let isSource = false;
            let isTarget = false;
            node.inputs()
                .filter((inputNode, idx) => inputNode && idx !== filterInputIdx)
                .forEach((inputNode) => {
                    const inputType = fromType(inputNode!!);
                    if (inputType === "source") {
                        isSource = true;
                    }
                    if (inputType === "target") {
                        isTarget = true;
                    }
                });
            return pathValidationType(node.node.nodeId, isSource, isTarget);
        default:
            return undefined;
    }
};

// Finds out if the target node is marked as "source", "target" or undefined (meaning not possible to determine) when replacing the target port.
const toType = (node: RuleEditorValidationNode, targetPortIdx: number): PathValidationType => {
    switch (node.node.pluginType) {
        case "ComparisonOperator":
            if (node.node.inputsCanBeSwitched) {
                // Return opposite type of other input if it exists
                const otherPort = targetPortIdx === 0 ? 1 : 0;
                const inputs = node.inputs();
                const otherInput = inputs[otherPort];
                if (otherInput == null) {
                    return undefined;
                } else {
                    switch (fromType(otherInput)) {
                        case undefined:
                            return undefined;
                        case "source":
                            return "target";
                        case "target":
                            return "source";
                    }
                }
            } else {
                // Else it only depends on the requested target port
                switch (targetPortIdx) {
                    case 0:
                        return "source";
                    case 1:
                        return "target";
                    default:
                        // If this happens then it's a bug
                        throw new RuleValidationError(
                            `Bug: Invalid connection to comparison operator ${node.node.label} on input port ${targetPortIdx} detected.`
                        );
                }
            }
            return undefined;
        case "TransformOperator":
            const targetNode = node.output();
            const targetNodePort = targetNode
                ? targetNode.inputs().findIndex((n) => n && node.node.nodeId === n.node.nodeId)
                : undefined;
            return targetNode && targetNodePort != null && targetNodePort >= 0
                ? toType(targetNode, targetNodePort)
                : undefined;
        default:
            return undefined;
    }
};

// Returns true if this is an invalid combination of source and target operator type
const invalidCombination = (s: PathValidationType, t: PathValidationType) => {
    const combination = `${s}_${t}`;
    switch (combination) {
        // Match invalid combinations
        case "source_target":
        case "target_source":
            return true;
        default:
            return false;
    }
};

/** For linking rules check that source and target paths are not mixed in transformations and go into the correct comparison port
 * Returns true if the connection is valid. */
const inputPathValidation = (
    fromRuleOperatorNode: RuleEditorValidationNode,
    toRuleOperatorNode: RuleEditorValidationNode,
    targetPortIdx: number
): boolean => {
    const sourceNodeFromType = fromType(fromRuleOperatorNode);
    const targetNodeToType = toType(toRuleOperatorNode, targetPortIdx);
    const targetNodeFromType = fromType(toRuleOperatorNode, targetPortIdx);
    if (invalidCombination(sourceNodeFromType, targetNodeToType)) {
        return false;
    } else return !invalidCombination(sourceNodeFromType, targetNodeFromType);
};

const pathValidationType = (nodeId: string, isSource: boolean, isTarget: boolean): PathValidationType => {
    if (isSource && isTarget) {
        // This should never happen and would be a bug
        throw new RuleValidationError("Transform operator detected with source and target inputs.", [{ nodeId }]);
    } else if (isSource) {
        return "source";
    } else if (isTarget) {
        return "target";
    }
};

type TabIdType = "all" | "transform" | "comparison" | "aggregation";

const sortAlphabetically = (ruleOpA: IRuleOperator, ruleOpB: IRuleOperator) =>
    ruleOpA.label.toLowerCase() < ruleOpB.label.toLowerCase() ? -1 : 1;

const sidebarTabs: Record<TabIdType, IRuleSideBarFilterTabConfig | IRuleSidebarPreConfiguredOperatorsTabConfig> = {
    all: {
        id: "all",
        label: "All", // TODO: i18n
        // set no icon for "All"
        filterAndSort: (ops) => ops,
        showOperatorsFromPreConfiguredOperatorTabsForQuery: true,
    },
    transform: {
        id: "transform",
        label: "Transform",
        icon: "operation-transform",
        filterAndSort: (ops) => ops.filter((op) => op.pluginType === "TransformOperator").sort(sortAlphabetically),
        showOperatorsFromPreConfiguredOperatorTabsForQuery: false,
    },
    comparison: {
        id: "comparison",
        label: "Comparison",
        icon: "operation-comparison",
        filterAndSort: (ops) => ops.filter((op) => op.pluginType === "ComparisonOperator").sort(sortAlphabetically),
        showOperatorsFromPreConfiguredOperatorTabsForQuery: false,
    },
    aggregation: {
        id: "aggregation",
        label: "Aggregation",
        icon: "operation-aggregation",
        filterAndSort: (ops) => ops.filter((op) => op.pluginType === "AggregationOperator").sort(sortAlphabetically),
        showOperatorsFromPreConfiguredOperatorTabsForQuery: false,
    },
};

const ruleUtils = {
    convertRuleOperator,
    convertRuleOperatorNodeToValueInput,
    convertToRuleOperatorNodeMap,
    extractOperatorNodeFromValueInput,
    fetchRuleOperatorNode,
    fromType,
    inputPathOperator,
    parameterSpecification,
    ruleLayout,
    validateConnection,
    sidebarTabs,
};

export default ruleUtils;
