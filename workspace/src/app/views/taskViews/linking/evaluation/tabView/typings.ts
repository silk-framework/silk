import { ILinkingRule } from "../../linking.types";

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

/** The statistics from the evaluation activity. */
export interface EvaluationActivityStats {
    // Number of generated/cached links in the activity
    nrLinks: number;
    // Number of source entities considered
    nrSourceEntities: number;
    // Number of target entities considered
    nrTargetEntities: number;
}

export interface EvaluationLinkInputValue<T = string[]> {
    source: Record<string, T>;
    target: Record<string, T>;
}

export type NodePath = number[];

export type ReferenceLinkType = "positive" | "negative" | "unlabeled";

export interface HoveredValuedType {
    path: string;
    isSourceEntity: boolean;
    value: string;
}

export const LinkEvaluationFilters = {
    positiveLinks: {
        key: "positiveLinks",
        label: "Confirmed",
    },
    negativeLinks: {
        key: "negativeLinks",
        label: "Declined",
    },
    undecidedLinks: {
        key: "undecidedLinks",
        label: "Uncertain",
    },
} as const;

export const LinkEvaluationSortByObj = {
    DESC: {
        source: "sourceEntityDesc",
        confidence: "scoreDesc",
        target: "targetEntityDesc",
    },
    ASC: {
        source: "sourceEntityAsc",
        confidence: "scoreAsc",
        target: "targetEntityAsc",
    },
} as const;

type linkSortByOrderType = keyof typeof LinkEvaluationSortByObj;

type linkSortByKeyType = keyof typeof LinkEvaluationSortByObj[linkSortByOrderType];

export type LinkEvaluationSortBy = typeof LinkEvaluationSortByObj[linkSortByOrderType][linkSortByKeyType];

export interface LinkRuleEvaluationResult {
    links: LinkingEvaluationResult[];
    linkRule: ILinkingRule;
    evaluationActivityStats: EvaluationActivityStats;
    resultStats: LinkRuleEvaluationResultStats;
    metaData: {
        sourceInputLabel: string;
        targetInputLabel: string;
    };
}

/**
 * Evaluation result statistics.
 */
interface LinkRuleEvaluationResultStats {
    /** The overall link count for the requested links before any filters are applied. */
    overallLinkCount: number;
    /** The link count after all filters (text, link decision etc.) have been applied. */
    filteredLinkCount: number;
}
