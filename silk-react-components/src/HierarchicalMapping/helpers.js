import _ from 'lodash';

export const MAPPING_RULE_TYPE_ROOT = 'root';
export const MAPPING_RULE_TYPE_OBJECT = 'object';
export const MAPPING_RULE_TYPE_DIRECT = 'direct';
export const MAPPING_RULE_TYPE_COMPLEX = 'complex';
export const MAPPING_RULE_TYPE_URI = 'uri';
export const MAPPING_RULE_TYPE_COMPLEX_URI = 'complexUri';

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

export const camelCase = string => {
        // add space before every capital letter
        string = string.replace(/([A-Z]+)/g, ' $1');
        // capital first letter
        string = string.charAt(0).toUpperCase() + string.slice(1);
        // remove leading space if exist
        if (_.startsWith(string, ' ')) {
            string = string.slice(1);
        }
        return string.replace(/_$/, "");
};

export const uriToLabel = uri => {
    const cleanUri = uri.toString().replace(/(^<+|>+$)/g, '');
    const hashparts = cleanUri.split('#');
    const uriparts = hashparts.length > 1 ? hashparts : cleanUri.split(':');

    let idx = 1;
    let label = uriparts[uriparts.length - idx];
    while (!label) {
        idx += 1;
        label = uriparts[uriparts.length - idx];
        if (idx < 0) {
            return cleanUri;
        }
    }
    return camelCase(label);
};

/**
 * @param label {string}
 * @param cleanUri {string}
 * @param uriLabel {string}
 *
 * @returns {{displayLabel: string, uri: null||string}}
 */
export const getRuleLabel = ({ label, cleanUri, uriLabel }) => {
	return {
		displayLabel: label ? label : uriLabel,
		uri: cleanUri.toLocaleString() !== label.toLocaleString() ? cleanUri : null
	};
};
