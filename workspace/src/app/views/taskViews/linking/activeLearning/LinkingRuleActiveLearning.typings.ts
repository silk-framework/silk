/** The steps of the active learning process. */
export type ActiveLearningStep = "config" | "linkLearning";

export interface ComparisonPairs {
    suggestedPairs: ComparisonPair[];
    selectedPairs: ComparisonPair[];
}

/** A pair of source and target paths that will be compared. */
export interface ComparisonPair {
    source: TypedPath;
    target: TypedPath;
    sourceExamples: string[][];
    targetExamples: string[][];
}

/** same but with ID */
export interface ComparisonPairWithId extends ComparisonPair {
    pairId: string;
}

/** A typed path. */
export interface TypedPath {
    /** Silk path expression. */
    path: string;
    /** Optional label for the path. */
    label?: string;
    /** Supported value types in active learning. */
    valueType?: "StringValueType" | "UriValueType";
}

export type ActiveLearningDecisions = "positive" | "negative" | "unlabeled";
