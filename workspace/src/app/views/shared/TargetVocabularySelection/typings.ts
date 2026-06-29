/** Vocabulary information. */
export interface IVocabularyInfo {
    // URI / ID of the vocabulary
    uri: string;
    // Optional (preferred) label of the vocabulary
    label?: string;
    // Number of classes
    nrClasses?: number;
    // Number of properties
    nrProperties?: number;
}

export interface IVocabularyInfoRequestResult {
    vocabularies: IVocabularyInfo[];
}

/** Batch lookup request for global vocabulary entries. */
export interface IVocabularyLookupRequest {
    projectId?: string;
    uris: string[];
}

/** Result for a single vocabulary lookup value. */
export interface IVocabularyLookupResult {
    input: string;
    resolved: boolean;
    invalid: boolean;
    uri: string;
    kind?: "class" | "property";
    label?: string;
    description?: string;
    prefixedUri?: string;
    graphUri?: string;
}

export interface IVocabularyLookupResponse {
    results: IVocabularyLookupResult[];
}
