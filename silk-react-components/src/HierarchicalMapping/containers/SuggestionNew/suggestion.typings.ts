export type SuggestionTypeValues = 'object' | 'value';
export interface ISuggestionTarget {
    type: SuggestionTypeValues;
    uri: string;
    confidence: number;
}

export interface ITransformedSuggestion {
    source: string;
    target: ISuggestionTarget[];
}
