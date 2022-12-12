type EvaluationChildValue = {
    operatorId: string;
    values: string[];
    error: string | null;
    children?: Array<Omit<LinkingEvaluationChildResult, "score">>;
};

export interface LinkingEvaluationChildResult {
    operatorId: string;
    score: number;
    sourceValue: EvaluationChildValue;
    targetValue: EvaluationChildValue;
}

export interface LinkingEvaluationResult {
    source: string;
    target: string;
    confidence: number;
    decision: "positive" | "negative" | "unlabeled";
    ruleValues: {
        operatorId: string;
        score: number;
        children: Array<LinkingEvaluationChildResult>;
    };
}

export interface EvaluationLinkInputValue {
    source: Record<string, string[]>;
    target: Record<string, string[]>;
}

export type NodePath = number[];
