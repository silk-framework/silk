import _ from 'lodash';

export const MAPPING_RULE_TYPE_ROOT = 'root';
export const MAPPING_RULE_TYPE_OBJECT = 'object';
export const MAPPING_RULE_TYPE_DIRECT = 'direct';
export const MAPPING_RULE_TYPE_COMPLEX = 'complex';
export const MAPPING_RULE_TYPE_URI = 'uri';
export const MAPPING_RULE_TYPE_COMPLEX_URI = 'complexUri';

export const isCopiableRule = type =>
    MAPPING_RULE_TYPE_DIRECT === type || MAPPING_RULE_TYPE_OBJECT === type || MAPPING_RULE_TYPE_COMPLEX === type || MAPPING_RULE_TYPE_ROOT === type;

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

export const trimValueLabelObject = object => {
    if (_.has(object, 'value') && _.isString(object.value)) {
        object.value = _.trim(object.value);
    }
    if (_.has(object, 'label')) {
        object.label = _.trim(object.label);
    }
    return object;
};

export const trimUriPattern = pattern => {
    return _.trim(pattern);
};

export const uriToLabel = uri => {
    const cleanUri = uri.replace(/(^<+|>+$)/g, '');
    const cutIndex = Math.max(cleanUri.lastIndexOf("#"), cleanUri.lastIndexOf("/"), cleanUri.lastIndexOf(":"), 0);
    const label = _.startCase(cleanUri.substr(cutIndex,cleanUri.length));
    return uri.toLowerCase() === label.toLowerCase() ? uri : label;
};

/**
 * @param label {string}
 * @param uriLabel {string}
 *
 * @returns {{displayLabel: string, uri: null||string}}
 */
export const getRuleLabel = ({ label, uri }) => {
    const cleanUri = uri.replace(/(^<+|>+$)/g, ''),
        uriLabel = uriToLabel(uri);
    return {
        displayLabel: label
            ? uri.toLowerCase() === label.toLowerCase() ? uri : label
            : uriLabel,
        uri: label
            ? cleanUri.toLowerCase() !== label.toLowerCase() ? cleanUri : null
            : uriLabel.toLowerCase() !== cleanUri.toLowerCase() ? cleanUri : null
    };
};
