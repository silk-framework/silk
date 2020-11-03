export type SuggestionTypeValues = 'object' | 'value';

export interface ISuggestionTarget {
    // the suggestion type, can be 'object' or 'value'
    type: SuggestionTypeValues;

    // the target uri
    uri: string;

    // the confidence number, this also shows ordering
    confidence: number;
}

export interface ITransformedSuggestion {
    // source name
    source: string;

    // targets array
    target: ISuggestionTarget[];
}

export interface IPlainObject {
    // plain object interface
    [key: string]: string | number
}

export interface IColumnFilters {
    // the label for filter
    label: string;
    // the action name
    action: string;
}

export interface ITableHeader {
    // the header column title, null values hidden
    header: string | null;

    // the column keycode
    key: string;
}

export interface ISortDirection {
    // column keycode
    column: string;
    // store ordering direction, '' - is default value
    modifier: 'asc' | 'desc' | ''
}
