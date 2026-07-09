import {
    IAggregationOperator,
    IComparisonOperator,
    IEntityLink,
    IEvaluationNode,
    IEvaluationValue,
    ISimilarityOperator,
} from "../linking/linking.types";
import { EvaluatedTransformEntity } from "../transform/transform.types";
import { IRuleBlockInput, IValueInput } from "../shared/rules/rule.typings";
import { IRuleBlockInputExample } from "./ruleBlock.types";

const createInputExample = (
    id: string,
    bindingValues: Array<{ operatorId: string; values: string[] }>,
    ruleBlockInput: IRuleBlockInput,
): IRuleBlockInputExample => {
    const portIdsByBindingNodeId = new Map(
        ruleBlockInput.bindings.map((binding) => [binding.input.id, binding.portId] as const),
    );
    const inputs = bindingValues.reduce<Record<string, string[]>>((currentInputs, bindingValue) => {
        const portId = portIdsByBindingNodeId.get(bindingValue.operatorId);
        if (portId) {
            currentInputs[portId] = bindingValue.values;
        }
        return currentInputs;
    }, {});
    return {
        id,
        inputs,
    };
};

const findRuleBlockInputInValueInput = (
    input: IValueInput | undefined,
    operatorId: string,
): IRuleBlockInput | undefined => {
    if (!input) {
        return undefined;
    }
    if (input.type === "ruleBlockInput") {
        return input.id === operatorId ? input : undefined;
    }
    if (input.type === "transformInput") {
        for (const child of input.inputs) {
            const match = findRuleBlockInputInValueInput(child, operatorId);
            if (match) {
                return match;
            }
        }
    }
    return undefined;
};

const findRuleBlockInputInSimilarityOperator = (
    operator: ISimilarityOperator | undefined,
    operatorId: string,
): IRuleBlockInput | undefined => {
    if (!operator) {
        return undefined;
    }
    if (operator.type === "Comparison") {
        const comparison = operator as IComparisonOperator;
        return (
            findRuleBlockInputInValueInput(comparison.sourceInput, operatorId) ??
            findRuleBlockInputInValueInput(comparison.targetInput, operatorId)
        );
    }
    const aggregation = operator as IAggregationOperator;
    for (const child of aggregation.inputs) {
        const match = findRuleBlockInputInSimilarityOperator(child, operatorId);
        if (match) {
            return match;
        }
    }
    return undefined;
};

const findTransformEvaluationSubTree = (
    evaluation: EvaluatedTransformEntity,
    operatorId: string,
): EvaluatedTransformEntity | undefined => {
    if (evaluation.operatorId === operatorId) {
        return evaluation;
    }
    for (const child of evaluation.children) {
        const match = findTransformEvaluationSubTree(child, operatorId);
        if (match) {
            return match;
        }
    }
    return undefined;
};

const createInputExamplesFromTransformEvaluations = (
    evaluations: EvaluatedTransformEntity[],
    ruleBlockNodeId: string,
    operatorTree?: IValueInput,
): IRuleBlockInputExample[] =>
    evaluations.flatMap((evaluation, index) => {
        const ruleBlockInput = findRuleBlockInputInValueInput(operatorTree, ruleBlockNodeId);
        const blackBoxRuleBlockNode = findTransformEvaluationSubTree(evaluation, ruleBlockNodeId);
        if (!ruleBlockInput || !blackBoxRuleBlockNode?.children?.length) {
            return [];
        }
        return [createInputExample(`inspection-example-${index + 1}`, blackBoxRuleBlockNode.children, ruleBlockInput)];
    });

const evaluationValueToTransformEntity = (evaluationValue: IEvaluationValue): EvaluatedTransformEntity => ({
    operatorId: evaluationValue.operatorId,
    values: evaluationValue.values,
    error: evaluationValue.error ?? null,
    stacktrace: evaluationValue.stacktrace,
    children: (evaluationValue.children ?? []).map(evaluationValueToTransformEntity),
});

const findLinkingRuleBlockValue = (
    evaluationValue: IEvaluationValue,
    operatorId: string,
): IEvaluationValue | undefined => {
    if (evaluationValue.operatorId === operatorId) {
        return evaluationValue;
    }
    for (const child of evaluationValue.children ?? []) {
        const match = findLinkingRuleBlockValue(child, operatorId);
        if (match) {
            return match;
        }
    }
    return undefined;
};

const findLinkingEvaluationSubTree = (
    evaluationNode: IEvaluationNode,
    operatorId: string,
): IEvaluationValue | undefined => {
    const comparison = evaluationNode as {
        sourceValue?: IEvaluationValue;
        targetValue?: IEvaluationValue;
        children?: IEvaluationNode[];
    };
    if (comparison.sourceValue) {
        const sourceMatch = findLinkingRuleBlockValue(comparison.sourceValue, operatorId);
        if (sourceMatch) {
            return sourceMatch;
        }
    }
    if (comparison.targetValue) {
        const targetMatch = findLinkingRuleBlockValue(comparison.targetValue, operatorId);
        if (targetMatch) {
            return targetMatch;
        }
    }
    for (const child of comparison.children ?? []) {
        const match = findLinkingEvaluationSubTree(child, operatorId);
        if (match) {
            return match;
        }
    }
    return undefined;
};

const createInputExamplesFromLinkingEvaluations = (
    links: Pick<IEntityLink, "ruleValues">[],
    ruleBlockNodeId: string,
    operatorTree?: ISimilarityOperator,
): IRuleBlockInputExample[] =>
    links
        .flatMap((link, index) => {
            const ruleBlockInput = findRuleBlockInputInSimilarityOperator(operatorTree, ruleBlockNodeId);
            const evaluationNode = link.ruleValues as IEvaluationNode | undefined;
            if (!ruleBlockInput || !evaluationNode?.operatorId) {
                return [];
            }
            const match = findLinkingEvaluationSubTree(evaluationNode, ruleBlockNodeId);
            if (!match?.children?.length) {
                return [];
            }
            return [createInputExample(`inspection-example-${index + 1}`, match.children, ruleBlockInput)];
        })
        .filter((example) => Object.keys(example.inputs).length > 0);

const ruleBlockInternalEvaluationUtils = {
    createInputExamplesFromLinkingEvaluations,
    createInputExamplesFromTransformEvaluations,
    evaluationValueToTransformEntity,
    findRuleBlockInputInSimilarityOperator,
    findRuleBlockInputInValueInput,
    findLinkingEvaluationSubTree,
    findTransformEvaluationSubTree,
};

export default ruleBlockInternalEvaluationUtils;
