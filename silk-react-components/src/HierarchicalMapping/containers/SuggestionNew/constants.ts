import { IColumnFilters } from "./suggestion.typings";

export const FILTER_ACTIONS = {
    SHOW_SELECTED: 'SHOW_SELECTED',
    SHOW_UNSELECTED: 'SHOW_UNSELECTED',
    SHOW_MATCHES: 'SHOW_MATCHES',
    SHOW_GENERATED: 'SHOW_GENERATED',
    SHOW_VALUE_MAPPINGS: 'SHOW_VALUE_MAPPINGS',
    SHOW_OBJECT_MAPPINGS: 'SHOW_OBJECT_MAPPINGS',
};

export const COLUMN_FILTERS: { [key: string]: IColumnFilters[] } = {
    checkbox: [{
        label: 'Show only selected items',
        action: FILTER_ACTIONS.SHOW_SELECTED
    }, {
        label: 'Show only unselected items',
        action: FILTER_ACTIONS.SHOW_UNSELECTED
    }],
    target: [{
        label: 'Show only matches',
        action: FILTER_ACTIONS.SHOW_MATCHES
    }, {
        label: 'Show only auto-generated properties',
        action: FILTER_ACTIONS.SHOW_GENERATED
    }],
    type: [{
        label: 'Show only value mappings',
        action: FILTER_ACTIONS.SHOW_VALUE_MAPPINGS
    }, {
        label: 'Show only object mappings',
        action: FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS
    }]
};

export const MAPPING_DEFAULTS = {
    // The default prefix for generated mapping properties
    DEFAULT_URI_PREFIX: 'urn:ruleProperty:'
};
