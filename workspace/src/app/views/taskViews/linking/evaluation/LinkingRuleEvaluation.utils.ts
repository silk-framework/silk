import { AggregationConfidence, ComparisonConfidence, IEvaluationNode, IEvaluationValue } from "../linking.types";
import { EvaluatedEntityLink, EvaluationResultType } from "./LinkingRuleEvaluation";
import {SampleError} from "../../../shared/SampleError/SampleError";

/** Turns an evaluation tree into a map operatorId => evaluation value */
const linkToValueMap = (link: EvaluatedEntityLink): Map<string, EvaluationResultType[number]> => {
    const valueMap = new Map<string, { error?: SampleError | null; value: string[] }>();
    const traverseEvaluationValueTree = (node: IEvaluationValue) => {
        let error: SampleError | undefined = undefined
        if(node.error != null) {
            error = {
                error: node.error!,
                values: (node.children ?? []).map(child => child.values),
                entity: "",
                stacktrace: node.stacktrace
            }
        }
        valueMap.set(node.operatorId, { value: node.values, error });
        node.children && node.children.forEach((c) => traverseEvaluationValueTree(c));
    };
    const traverseEvaluationTree = (node: IEvaluationNode) => {
        if ((node as AggregationConfidence).children) {
            valueMap.set(node.operatorId, { value: [`Score: ${node.score ?? ""}`] });
            (node as AggregationConfidence).children.forEach((n) => traverseEvaluationTree(n));
        } else {
            const comparison = node as ComparisonConfidence;
            let error: SampleError | undefined = undefined
            if(comparison.sourceValue.error != null) {
                const source = comparison.sourceValue
                const target = comparison.targetValue
                error = {
                    // Error is attached to the source input
                    error: source.error!,
                    values: [source.values, target.values],
                    entity: "",
                    stacktrace: source.stacktrace
                }
            }
            valueMap.set(comparison.operatorId, {
                value: [`Score: ${node.score ?? ""}`],
                error,
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
