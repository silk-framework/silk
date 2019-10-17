export const MESSAGES = {
    RULE_ID: {
        CREATE: 'ruleId.create',
    },
    RULE_VIEW: {
        CHANGE: 'ruleView.change',
        UNCHANGED: 'ruleView.unchanged',
        CLOSE: 'ruleView.close',
        DISCARD_ALL: 'ruleView.discardAll',
        TOGGLE: 'ruleView.toggle',
    },
    RULE: {
        SUGGESTIONS: {
            PROGRESS: 'rule.suggestions.progress',
        },
        COPY: 'rule.copy',
        REQUEST_ORDER: 'rule.requestOrder',
    },
    TREE_NAV: {
        TOGGLE_VISIBILITY: 'toggleVisibility',
    },
    MAPPING: {
        CREATE: 'mapping.create',
        SHOW_SUGGESTIONS: 'showSuggestions',
    },
    RELOAD: 'reload',
    TOGGLE_DETAILS: 'toggleDetails',
};

export const MAPPING_RULE_TYPE_DIRECT = 'direct';
export const MAPPING_RULE_TYPE_COMPLEX = 'complex';
export const MAPPING_RULE_TYPE_URI = 'uri';
export const MAPPING_RULE_TYPE_COMPLEX_URI = 'complexUri';
export const MAPPING_RULE_TYPE_ROOT = 'root';
export const MAPPING_RULE_TYPE_OBJECT = 'object';

export const isCopiableRule = type =>
    MAPPING_RULE_TYPE_DIRECT === type || MAPPING_RULE_TYPE_OBJECT === type || MAPPING_RULE_TYPE_COMPLEX === type || MAPPING_RULE_TYPE_ROOT === type;

export const isClonableRule = type =>
    MAPPING_RULE_TYPE_DIRECT === type || MAPPING_RULE_TYPE_OBJECT === type || MAPPING_RULE_TYPE_COMPLEX === type;

export const isObjectMappingRule = type =>
    MAPPING_RULE_TYPE_ROOT === type || MAPPING_RULE_TYPE_OBJECT === type;

export const SUGGESTION_TYPES = ['value', 'object'];

export const LABELED_SUGGESTION_TYPES = [
    {
        value: SUGGESTION_TYPES[0],
        label: 'Value mapping',
    },
    {
        value: SUGGESTION_TYPES[1],
        label: 'Object mapping',
    },
];
