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
    ruleValues: {
        operatorId: string;
        score: number;
        children: Array<LinkingEvaluationChildResult>;
    };
}

export interface LinkingInputValue {
    type: string;
    id: string;
    function: string;
    path?: string;
    inputs: Array<{ type: string; id: string; path: string }>;
}
export interface LinkingEvaluationRule {
    operator: {
        id: string;
        type: string;
        weight: number;
        aggregator: string;
        sourceInput?: LinkingInputValue;
        targetInput?: LinkingInputValue;
        inputs: Array<{
            id: string;
            type: string;
            weight: number;
            threshold: number;
            indexing: boolean;
            metric: string;
            parameters: {
                minChar: string;
                maxChar: string;
            };
            sourceInput: LinkingInputValue;
            targetInput: LinkingInputValue;
        }>;
    } | null;
}

export interface EvaluationLinkInputValue {
    source: Record<string, string[]>;
    target: Record<string, string[]>;
}
