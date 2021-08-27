/** Vocabulary information. */
export interface IVocabularyInfo {
    // URI / ID of the vocabulary
    uri: string;
    // Optional (preferred) label of the vocabulary
    label?: string;
    // Number of classes
    nrClasses: number;
    // Number of properties
    nrProperties: number;
}

export interface IVocabularyInfoRequestResult {
    vocabularies: IVocabularyInfo[];
}
