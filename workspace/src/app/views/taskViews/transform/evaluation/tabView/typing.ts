import type { IValueInput } from "../../../shared/rules/rule.typings";

export interface EvaluatedEntityOperator {
    operatorId: string;
    values: string[];
    error?: string;
    children: EvaluatedEntityOperator[];
}

export interface EvaluatedEntity {
    uris: string[];
    values: EvaluatedEntityOperator[];
}

export type EvaluatedRuleOperator = IValueInput;

export interface EvaluatedURIRule {
    type: string;
    id: string;
    rules?: {
        propertyRules: Array<{
            id: string;
            type: string;
            operator: EvaluatedRuleOperator;
        }>;
    };
    operator?: EvaluatedRuleOperator;
}

export interface EvaluatedComplexRule extends EvaluatedURIRule {
    mappingTarget: {
        uri: string;
        valueType: {
            nodeType: string;
        };
    };
}
export interface EvaluatedRuleEntityResult {
    rules: Array<EvaluatedURIRule | EvaluatedComplexRule>;
    evaluatedEntities: EvaluatedEntity[];
}
