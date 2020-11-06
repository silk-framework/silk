export type SuggestionTypeValues = 'object' | 'value';

// the object which pass parent for adding new suggestion
export interface IAddedSuggestion {
    // selected source
    source: string;

    // selected target uri
    targetUri: string;

    // target type
    type: SuggestionTypeValues;
}

export interface ITargetWithSelected extends ISuggestionTarget {
    // indicate selected target
    _selected: boolean;
}

export interface IPageSuggestion extends ITransformedSuggestion {
    // modified target element
    target: ITargetWithSelected[]
}

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
