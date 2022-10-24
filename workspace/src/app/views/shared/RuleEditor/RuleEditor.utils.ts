import getColorConfiguration from "@eccenca/gui-elements/src/common/utils/getColorConfiguration";
import { IRuleOperator, IRuleOperatorNode } from "./RuleEditor.typings";

/** Default function to turn a rule operator into a rule node. */
const convertRuleOperatorToRuleNode = (ruleOperator: IRuleOperator): Omit<IRuleOperatorNode, "nodeId"> => {
    return {
        inputs: [],
        label: ruleOperator.label,
        parameters: Object.fromEntries(
            Object.entries(ruleOperator.parameterSpecification).map(([paramId, paramSpec]) => {
                return [paramId, paramSpec.defaultValue];
            })
        ),
        pluginId: ruleOperator.pluginId,
        pluginType: ruleOperator.pluginType,
        portSpecification: ruleOperator.portSpecification,
        tags: ruleOperator.tags,
        description: ruleOperator.description,
        markdownDocumentation: ruleOperator.markdownDocumentation,
        inputsCanBeSwitched: ruleOperator.inputsCanBeSwitched,
    };
};

/** Fetch operator node for a specific plugin. */
const getOperatorNode = (pluginId: string, operatorMap: Map<string, IRuleOperator[]>, pluginType?: string) => {
    const operatorPlugins = operatorMap.get(pluginId);
    if (!operatorPlugins) {
        console.warn("No plugin operator with ID " + pluginId + " found!");
    } else {
        return pluginType ? operatorPlugins.find((plugin) => plugin.pluginType === pluginType) : operatorPlugins[0];
    }
};

/** Returns the background color that should be used for operators of a specific rule type. */
export const linkingRuleOperatorTypeColorFunction: () => (id: string) => string | undefined = () => {
    const tabColors = getColorConfiguration("react-flow-linking");
    return (id: string): string | undefined => {
        switch (id) {
            case "sourcePathInput":
            case "sourcePaths":
                return tabColors.sourcepathNodeBright;
            case "targetPathInput":
            case "targetPaths":
                return tabColors.targetpathNodeBright;
            case "PathInputOperator":
                return tabColors.sourcepathNodeBright;
            case "ComparisonOperator":
            case "comparison":
                return tabColors.comparatorNodeBright;
            case "TransformOperator":
            case "transform":
                return tabColors.transformationNodeBright;
            case "AggregationOperator":
            case "aggregation":
                return tabColors.aggregatorNodeBright;
            default:
                return undefined;
        }
    };
};

const utils = {
    defaults: {
        convertRuleOperatorToRuleNode,
    },
    getOperatorNode,
    linkingRuleOperatorTypeColorFunction,
};

export default utils;
