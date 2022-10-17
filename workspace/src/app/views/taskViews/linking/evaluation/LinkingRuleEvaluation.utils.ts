import { AggregationConfidence, ComparisonConfidence, IEvaluationNode, IEvaluationValue } from "../linking.types";
import { EvaluatedEntityLink, EvaluationResultType } from "./LinkingRuleEvaluation";

/** Turns an evaluation tree into a map operatorId => evaluation value */
const linkToValueMap = (link: EvaluatedEntityLink): Map<string, EvaluationResultType[number]> => {
    const valueMap = new Map<string, { error?: string | null; value: string[] }>();
    const traverseEvaluationValueTree = (node: IEvaluationValue) => {
        valueMap.set(node.operatorId, { value: node.values, error: node.error });
        node.children && node.children.forEach((c) => traverseEvaluationValueTree(c));
    };
    const traverseEvaluationTree = (node: IEvaluationNode) => {
        if ((node as AggregationConfidence).children) {
            valueMap.set(node.operatorId, { value: [`Score: ${node.score ?? ""}`] });
            (node as AggregationConfidence).children.forEach((n) => traverseEvaluationTree(n));
        } else {
            const comparison = node as ComparisonConfidence;
            valueMap.set(comparison.operatorId, {
                value: [`Score: ${node.score ?? ""}`],
                error: comparison.sourceValue.error,
            });
            traverseEvaluationValueTree(comparison.sourceValue);
            traverseEvaluationValueTree(comparison.targetValue);
        }
    };
    link.ruleValues &&
        (link.ruleValues as IEvaluationNode).operatorId &&
        traverseEvaluationTree(link.ruleValues as IEvaluationNode);
    return valueMap;
};

const utils = {
    linkToValueMap,
};

export default utils;
