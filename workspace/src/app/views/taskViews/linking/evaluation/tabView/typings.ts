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

export interface EvaluationLinkInputValue<T = string[]> {
    source: Record<string, T>;
    target: Record<string, T>;
}

export type NodePath = number[];

export type ReferenceLinkType = "positive" | "negative" | "unlabeled";
