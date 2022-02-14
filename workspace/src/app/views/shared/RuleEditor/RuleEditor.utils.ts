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
    };
};

const utils = {
    defaults: {
        convertRuleOperatorToRuleNode,
    },
};

export default utils;
