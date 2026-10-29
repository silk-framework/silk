import countRuleBlocksRecursive from "./getRuleBlocks";
import getUriOperatorsRecursive from "./getUriOperators";

const buildRuleFormulaStats = ({ paths, operator, getPluginDetailLabel }) => {
    const operators = getUriOperatorsRecursive(operator, []);
    return {
        pathCount: paths.length,
        operatorCount: operators.length,
        operatorLabels: operators.map(getPluginDetailLabel).join(", "),
        ruleBlockCount: countRuleBlocksRecursive(operator),
    };
};

export default buildRuleFormulaStats;
