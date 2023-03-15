interface EvaluatedEntityOperator {
    operatorId: string;
    values: string[];
    error?: string;
    children: EvaluatedEntityOperator[];
}

interface EvaluatedEntity {
    uris: string[];
    values: EvaluatedEntityOperator[];
}

type ruleOperatorTypes = "transformInput" | "pathInput";

interface EvaluatedRule {
    type: string;
    id: string;
    operator: {
        type: ruleOperatorTypes;
        id: string;
        function?: string;
        path?: string;
        inputs: Array<EvaluatedRule["operator"]>;
        parameters: {
            glue?: string;
            missingValuesAsEmptyStrings?: string;
            value?: string;
        };
    };
}

export interface EvaluatedRuleEntityResult {
    rules: EvaluatedRule[];
    evaluatedEntities: EvaluatedEntity[];
}
