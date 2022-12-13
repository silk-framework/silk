import { IColumnFilters } from "./suggestion.typings";

export const FILTER_ACTIONS = {
    SHOW_SELECTED: "SHOW_SELECTED",
    SHOW_UNSELECTED: "SHOW_UNSELECTED",
    SHOW_MATCHES: "SHOW_MATCHES",
    SHOW_GENERATED: "SHOW_GENERATED",
    SHOW_VALUE_MAPPINGS: "SHOW_VALUE_MAPPINGS",
    SHOW_OBJECT_MAPPINGS: "SHOW_OBJECT_MAPPINGS",
    SHOW_UNUSED_SOURCE_PATHS_ONLY: "SHOW_UNUSED_SOURCE_PATHS_ONLY",
    SHOW_USED_SOURCE_PATHS_ONLY: "SHOW_USED_SOURCE_PATHS_ONLY",
};

export const COLUMN_FILTERS: { [key: string]: IColumnFilters[] } = {
    checkbox: [
        {
            label: "Show only selected items",
            action: FILTER_ACTIONS.SHOW_SELECTED,
            selectable: "always",
        },
        {
            label: "Show only unselected items",
            action: FILTER_ACTIONS.SHOW_UNSELECTED,
            selectable: "always",
        },
    ],
    source: [
        {
            label: "Show only non-mapped source paths",
            action: FILTER_ACTIONS.SHOW_UNUSED_SOURCE_PATHS_ONLY,
            selectable: "sourceViewOnly",
        },
        {
            label: "Show only already mapped source paths",
            action: FILTER_ACTIONS.SHOW_USED_SOURCE_PATHS_ONLY,
            selectable: "sourceViewOnly",
        },
    ],
    target: [
        {
            label: "Show only matches",
            action: FILTER_ACTIONS.SHOW_MATCHES,
            selectable: "sourceViewOnly",
        },
        {
            label: "Show only auto-generated properties",
            action: FILTER_ACTIONS.SHOW_GENERATED,
            selectable: "sourceViewOnly",
        },
    ],
    type: [
        {
            label: "Show only value mappings",
            action: FILTER_ACTIONS.SHOW_VALUE_MAPPINGS,
            selectable: "always",
        },
        {
            label: "Show only object mappings",
            action: FILTER_ACTIONS.SHOW_OBJECT_MAPPINGS,
            selectable: "always",
        },
    ],
};

export const LOCAL_STORAGE_KEYS = {
    SELECTED_PREFIX: "suggestion_selected_prefix",
};

export const MAPPING_DEFAULTS = {
    // The default prefix for generated mapping properties
    DEFAULT_URI_PREFIX: "urn:ruleProperty:",
};
