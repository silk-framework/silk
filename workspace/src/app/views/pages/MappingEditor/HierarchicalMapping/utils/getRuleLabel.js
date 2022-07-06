import _ from 'lodash';

/**
 * @param label {string}
 * @param uriLabel {string}
 *
 * @returns {{displayLabel: string, uri: null||string}}
 */
export const getRuleLabel = ({label, uri}) => {
    const cleanUri = uri.replace(/(^<+|>+$)/g, '');
    const cutIndex = Math.max(cleanUri.lastIndexOf('#'), cleanUri.lastIndexOf('/'), cleanUri.lastIndexOf(':'), 0);
    
    const _label = _.startCase(cleanUri.substr(cutIndex, cleanUri.length));
    const uriLabel = uri.toLowerCase() === _label.toLowerCase() ? uri : _label;
    
    return {
        displayLabel: label
            ? uri.toLowerCase() === label.toLowerCase() ? uri : label
            : uriLabel,
        uri: label
            ? cleanUri.toLowerCase() !== label.toLowerCase() ? cleanUri : null
            : uriLabel.toLowerCase() !== cleanUri.toLowerCase() ? cleanUri : null,
    };
};
