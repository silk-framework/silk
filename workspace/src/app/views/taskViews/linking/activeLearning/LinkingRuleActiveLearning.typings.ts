import { LinkType, ReferenceLinksStats } from "../referenceLinks/LinkingRuleReferenceLinks.typing";
import { IEvaluatedReferenceLinksScore, ILinkingRule, OptionallyLabelledParameter } from "../linking.types";

/** The steps of the active learning process. */
export type ActiveLearningStep = "config" | "linkLearning";

export interface ComparisonPairs {
    suggestedPairs: ComparisonPair[];
    selectedPairs: ComparisonPair[];
    warnings: string[];
}

/** A pair of source and target paths that will be compared. */
export interface ComparisonPair {
    source: TypedPath;
    target: TypedPath;
    // TODO: nested arrays needed?
    sourceExamples: string[];
    targetExamples: string[];
    /** A confidence score that goes from -1.0 to 1.0 (not matching to optimal match) */
    score?: number;
    /** The type the comparison pair is compared with, e.g. string comparison, date comparison etc. */
    comparisonType?: "string" | "number" | "dateTime" | string;
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
    /** Reference link for source entity */
    sourceBrowserUrl?: string;
    /** Reference link for target entity */
    targetBrowserUrl?: string;
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

/** The best currently learned rule returned by the bestRule endpoint. */
export type ActiveLearningBestRule = OptionallyLabelledParameter<ILinkingRule> & {
    evaluationResult: IEvaluatedReferenceLinksScore;
};

/** A reference link from the active learning reference links endpoint that contains an evaluation of the comparison pairs. */
export interface ActiveLearningReferenceLink extends ActiveLearningLinkCandidate {
    /** The label of the reference link. */
    decision: LinkType;
    /** The confidence score, -1.0 to 1.0 */
    score?: number;
}

/** Reference links returned from the active learning reference links endpoint. */
export interface ActiveLearningReferenceLinks {
    links: ActiveLearningReferenceLink[];
}

export interface LinkingValuePathExampleValues {
    exampleValues: string[];
}
