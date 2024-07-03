/**
 * Convert to backend model:
 */

import {
    IRuleOperator,
    IRuleOperatorNode,
    IRuleSidebarPreConfiguredOperatorsTabConfig,
    RuleEditorValidationNode,
    RuleValidationError,
} from "../../shared/RuleEditor/RuleEditor.typings";
import { IValueInput, PathWithMetaData } from "../shared/rules/rule.typings";
import {
    IAggregationOperator,
    IComparisonOperator,
    ILinkingRule,
    ILinkingTaskParameters,
    ISimilarityOperator,
    optionallyLabelledParameterToValue,
} from "./linking.types";
import { RuleOperatorFetchFnType } from "../../shared/RuleEditor/RuleEditor";
import ruleUtils from "../shared/rules/rule.utils";
import { RuleOperatorType, TaskPlugin } from "@ducks/shared/typings";
import linkingRuleRequests from "./LinkingRuleEditor.requests";
import { IPreConfiguredRuleOperator } from "../../shared/RuleEditor/view/sidebar/RuleEditorOperatorSidebar.typings";
import { ruleEditorNodeParameterValue } from "../../shared/RuleEditor/model/RuleEditorModel.typings";
import { IStickyNote } from "../shared/task.typings";

/**
 * Convert to editor model:
 */

const comparatorInputs = (comparison: IComparisonOperator): IValueInput[] => [
    comparison.sourceInput,
    comparison.targetInput,
];

const aggregatorInputs = (aggregator: IAggregationOperator): ISimilarityOperator[] => aggregator.inputs;

const REVERSE_PARAMETER_ID = "reverse";

/**
 * Extracts and adds a single operator node to the array, recursively executes on its children.
 * @return the ID of the operator node
 */

const extractSimilarityOperatorNode = (
    operator: ISimilarityOperator | undefined,
    result: IRuleOperatorNode[],
    ruleOperator: RuleOperatorFetchFnType
): string | undefined => {
    if (operator) {
        const isComparison = operator.type === "Comparison";
        const inputs = isComparison
            ? comparatorInputs(operator as IComparisonOperator).map((input, idx) =>
                  ruleUtils.extractOperatorNodeFromValueInput(input, result, idx > 0, ruleOperator)
              )
            : aggregatorInputs(operator as IAggregationOperator).map((input) =>
                  extractSimilarityOperatorNode(input, result, ruleOperator)
              );
        const pluginId = isComparison
            ? (operator as IComparisonOperator).metric
            : (operator as IAggregationOperator).aggregator;
        const pluginType: RuleOperatorType = isComparison ? "ComparisonOperator" : "AggregationOperator";
        const threshold = (comparison: IComparisonOperator): string => {
            let t = comparison.threshold.toString();
            if (t.length === 1) {
                // Threshold should be recognized as a float value
                t = comparison.threshold.toFixed(1);
            }
            return t;
        };
        const reverseParameterValue = () =>
            operator.parameters[REVERSE_PARAMETER_ID]?.["value"] ?? operator.parameters[REVERSE_PARAMETER_ID];
        const inputsCanBeSwitched = isComparison && reverseParameterValue() != null;
        const switchInputs = inputsCanBeSwitched && reverseParameterValue() === "true";
        if (switchInputs) {
            inputs.reverse();
        }
        const additionalParameters = isComparison
            ? {
                  threshold: threshold(operator as IComparisonOperator),
                  weight: operator.weight.toString(),
              }
            : {
                  weight: operator.weight.toString(),
              };

        const op = ruleOperator(pluginId, pluginType);
        result.push({
            nodeId: operator.id,
            label: op?.label ?? pluginId,
            pluginType,
            pluginId,
            inputs,
            parameters: {
                ...operator.parameters,
                ...additionalParameters,
            },
            portSpecification: {
                minInputPorts: isComparison ? 2 : 1,
                maxInputPorts: isComparison ? 2 : undefined,
            },
            tags: [operator.type],
            description: op?.description,
            inputsCanBeSwitched,
            markdownDocumentation: op?.markdownDocumentation,
        });
        return operator.id;
    }
};

/** gets preloaded ui sticky notes */
const getStickyNotes = (linkSpec: TaskPlugin<ILinkingTaskParameters>): IStickyNote[] =>
    (linkSpec && optionallyLabelledParameterToValue(linkSpec.parameters.rule).uiAnnotations.stickyNotes) || [];

/** Converts the linking task rule to the internal representation. */
const convertLinkingTaskToRuleOperatorNodes = (
    linkSpec: TaskPlugin<ILinkingTaskParameters>,
    ruleOperator: RuleOperatorFetchFnType
): IRuleOperatorNode[] => {
    const rule = optionallyLabelledParameterToValue(linkSpec.parameters.rule);
    return convertLinkingRuleToRuleOperatorNodes(rule, ruleOperator);
};

/** Convert a linking rule to rule operator nodes. */
const convertLinkingRuleToRuleOperatorNodes = (
    linkRule: ILinkingRule,
    ruleOperator: RuleOperatorFetchFnType
): IRuleOperatorNode[] => {
    const operatorNodes: IRuleOperatorNode[] = [];
    extractSimilarityOperatorNode(linkRule.operator, operatorNodes, ruleOperator);
    const nodePositions = linkRule.layout.nodePositions;
    operatorNodes.forEach((node) => {
        const [x, y] = nodePositions[node.nodeId] ?? [null, null];
        node.position = x !== null ? { x, y } : undefined;
    });
    return operatorNodes;
};

// Converts a rule operator node to a rule validation node
const fromType = (
    ruleOperatorNode: IRuleOperatorNode,
    ruleOperatorNodes: Map<string, IRuleOperatorNode>
): "source" | "target" | undefined => {
    const convertNode = (ruleOperatorNode: IRuleOperatorNode): RuleEditorValidationNode => {
        return {
            node: ruleOperatorNode,
            inputs: () => {
                return ruleOperatorNode.inputs.map((input) => {
                    return input && ruleOperatorNodes.has(input)
                        ? convertNode(ruleOperatorNodes.get(input)!!)
                        : undefined;
                });
            },
            // Output is unimportant
            output: () => undefined,
        };
    };
    return ruleUtils.fromType(convertNode(ruleOperatorNode));
};

const convertRuleOperatorNodeToSimilarityOperator = (
    ruleOperatorNode: IRuleOperatorNode | undefined,
    ruleOperatorNodes: Map<string, IRuleOperatorNode>
): ISimilarityOperator | undefined => {
    if (ruleOperatorNode) {
        if (ruleOperatorNode.pluginType === "ComparisonOperator") {
            if (ruleOperatorNode.inputs.length !== 2 || !ruleOperatorNode.inputs.every((input) => input != null)) {
                throw new RuleValidationError(
                    `Comparison operator '${ruleOperatorNode.label}' must have 2 inputs, but is missing at least 1 input!`,
                    [ruleOperatorNode]
                );
            }
            const comparison: IComparisonOperator = {
                metric: ruleOperatorNode.pluginId,
                sourceInput: ruleUtils.convertRuleOperatorNodeToValueInput(
                    ruleUtils.fetchRuleOperatorNode(ruleOperatorNode.inputs[0]!!, ruleOperatorNodes, ruleOperatorNode),
                    ruleOperatorNodes
                ),
                targetInput: ruleUtils.convertRuleOperatorNodeToValueInput(
                    ruleUtils.fetchRuleOperatorNode(ruleOperatorNode.inputs[1]!!, ruleOperatorNodes, ruleOperatorNode),
                    ruleOperatorNodes
                ),
                id: ruleOperatorNode.nodeId,
                indexing: true, // FIXME: Should this be part of the config in the UI?
                parameters: Object.fromEntries(
                    Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                        parameterKey,
                        parameterValue ?? "",
                    ])
                ),
                type: "Comparison",
                threshold: parseFloat(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["threshold"])!!),
                weight: parseInt(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["weight"])!!),
            };
            if (ruleOperatorNode.inputsCanBeSwitched && ruleOperatorNode.inputs[0] != null) {
                // Set reverse parameter correctly. Either the first input has a target path or the second input has a source path.
                const reverseNeeded = (inputIdx: 0 | 1): boolean => {
                    const inputId = ruleOperatorNode.inputs[inputIdx];
                    const input = inputId ? ruleOperatorNodes.get(inputId) : undefined;
                    const typeNeedingReversing = inputIdx === 0 ? "target" : "source";
                    return input ? fromType(input, ruleOperatorNodes) === typeNeedingReversing : false;
                };
                const reverse = reverseNeeded(0) || reverseNeeded(1);
                comparison.parameters[REVERSE_PARAMETER_ID] = `${reverse}`;
                // Switch inputs if they have the order target-source. The reverse parameter is handling the correct order.
                if (reverse) {
                    const sourceInput = comparison.sourceInput;
                    comparison.sourceInput = comparison.targetInput;
                    comparison.targetInput = sourceInput;
                }
            }
            return comparison;
        } else {
            const aggregation: IAggregationOperator = {
                id: ruleOperatorNode.nodeId,
                aggregator: ruleOperatorNode.pluginId,
                inputs: ruleOperatorNode.inputs
                    .filter((i) => i != null)
                    .map(
                        (i) =>
                            convertRuleOperatorNodeToSimilarityOperator(
                                ruleUtils.fetchRuleOperatorNode(i!!, ruleOperatorNodes, ruleOperatorNode),
                                ruleOperatorNodes
                            )!!
                    ),
                parameters: Object.fromEntries(
                    Object.entries(ruleOperatorNode.parameters).map(([parameterKey, parameterValue]) => [
                        parameterKey,
                        parameterValue ?? "",
                    ])
                ),
                type: "Aggregation",
                weight: parseInt(ruleEditorNodeParameterValue(ruleOperatorNode.parameters["weight"])!!),
            };
            return aggregation;
        }
    }
};

/** Input path tab configuration. */
const inputPathTab = (
    projectId: string,
    linkingTaskId: string,
    baseOperator: IRuleOperator,
    sourceOrTarget: "source" | "target",
    errorHandler: (err) => any
): IRuleSidebarPreConfiguredOperatorsTabConfig => {
    const category = sourceOrTarget === "source" ? "Source path" : "Target path";
    const inputPathTabConfig: IRuleSidebarPreConfiguredOperatorsTabConfig<PathWithMetaData> = {
        id: `${sourceOrTarget}Paths`,
        icon: sourceOrTarget === "source" ? "data-sourcepath" : "data-targetpath",
        label: sourceOrTarget === "source" ? "Source paths" : "Target paths",
        defaultOperators: [
            {
                value: "",
                valueType: "",
                label: category,
                _idPrefix: sourceOrTarget,
            },
        ],
        fetchOperators: async (langPref: string) => {
            try {
                return (
                    (
                        (
                            await linkingRuleRequests.fetchLinkingCachedPaths(
                                projectId,
                                linkingTaskId,
                                sourceOrTarget,
                                true,
                                langPref
                            )
                        ).data as PathWithMetaData[]
                    )
                        // We need to make the IDs unique because values are not sufficient enough
                        .map((p) => ({ ...p, _idPrefix: sourceOrTarget }))
                );
            } catch (ex) {
                errorHandler(ex);
            }
        },
        convertToOperator: (path: PathWithMetaData): IPreConfiguredRuleOperator => {
            const { pluginId, pluginType, icon } = baseOperator;
            return {
                pluginId,
                pluginType,
                icon,
                label: path.label ?? path.value,
                description: path.label ? path.value : undefined,
                categories: [category],
                parameterOverwrites: {
                    path: path.label ? { value: path.value, label: path.label } : path.value,
                },
                tags: path.valueType ? [path.valueType] : [],
                inputsCanBeSwitched: false,
            };
        },
        isOriginalOperator: (listItem) => (listItem as PathWithMetaData)._idPrefix === sourceOrTarget,
        itemSearchText: (listItem: PathWithMetaData, mergedWithOtherOperators: boolean) =>
            `${listItem.label ?? ""} ${listItem.value} ${listItem.valueType} ${
                mergedWithOtherOperators ? category : ""
            }`.toLowerCase(),
        itemLabel: (listItem: PathWithMetaData) => listItem.label ?? listItem.value,
        itemId: (listItem: PathWithMetaData) => {
            return `${sourceOrTarget}_${listItem.value}`;
        },
    };
    return inputPathTabConfig;
};

/** Constructs the rule tree of a linkage rule. This is data structure that is send to the backend. */
export const constructLinkageRuleTree = (ruleOperatorNodes: IRuleOperatorNode[]): ISimilarityOperator | undefined => {
    const [operatorNodeMap, rootNodes] = ruleUtils.convertToRuleOperatorNodeMap(ruleOperatorNodes, true);
    if (
        rootNodes.length === 1 &&
        rootNodes[0].pluginType !== "ComparisonOperator" &&
        rootNodes[0].pluginType !== "AggregationOperator"
    ) {
        throw new RuleValidationError(
            "Rule tree root must either be an aggregation or comparison!",
            rootNodes.map((node) => ({
                nodeId: node.nodeId,
                message: `Root node '${node.label}' is a '${node.pluginType}', but must be either a comparison or aggregation.`,
            }))
        );
    }

    return rootNodes.length === 1
        ? convertRuleOperatorNodeToSimilarityOperator(rootNodes[0], operatorNodeMap)
        : undefined;
};

const linkingRuleUtils = {
    convertRuleOperatorNodeToSimilarityOperator,
    convertLinkingTaskToRuleOperatorNodes,
    convertLinkingRuleToRuleOperatorNodes,
    inputPathTab,
    constructLinkageRuleTree,
    optionallyLabelledParameterToValue,
    getStickyNotes,
};

export default linkingRuleUtils;
