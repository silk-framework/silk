/** A property of a property pair used for the active learning configuration. */
export interface CandidateProperty {
    value: string;
    label?: string;
    type?: string; // TODO: types
    exampleValues: string[];
}

/** A pair of candidate properties. */
export interface CandidatePropertyPair {
    pairId: string;
    left: CandidateProperty;
    right: CandidateProperty;
}
