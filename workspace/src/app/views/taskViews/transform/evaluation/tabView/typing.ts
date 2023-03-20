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

type ruleOperatorTypes = "transformInput" | "pathInput";

export interface EvaluatedURIRule {
    type: string;
    id: string;
    operator: {
        type: ruleOperatorTypes;
        id: string;
        function?: string;
        path?: string;
        inputs: Array<EvaluatedURIRule["operator"]>;
        parameters: {
            glue?: string;
            missingValuesAsEmptyStrings?: string;
            value?: string;
        };
    };
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
