import { ReferenceLinksStats } from "../referenceLinks/LinkingRuleReferenceLinks.typing";

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
    /** A confidence score that goes from -1.0 to 1.0 (not matching to optimal match) */
    score?: number;
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

/** A link candidate. */
export interface ActiveLearningLinkCandidate {
    source: string;
    target: string;
    comparisons: ComparisonPair[];
}

/** Information about users that have contributed to an active learning session. */
interface UserInfo {
    uri: string;
    label: string;
}

/** Information about the current active learning session. */
export interface ActiveLearningSessionInfo {
    users: UserInfo[];
    referenceLinks: ReferenceLinksStats;
}
