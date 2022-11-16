type EvaluationChildValue = {
    operatorId: string;
    values: string[];
    error: string | null;
    children?: Array<Omit<LinkingEvaluationChildResult, "score">>;
};

interface LinkingEvaluationChildResult {
    operatorId: string;
    score: number;
    sourceValue: EvaluationChildValue;
    targetValue: EvaluationChildValue;
}

export interface LinkingEvaluationResult {
    source: string;
    target: string;
    confidence: number;
    ruleValues: {
        operatorId: string;
        score: number;
        children: Array<LinkingEvaluationChildResult>;
    };
}
