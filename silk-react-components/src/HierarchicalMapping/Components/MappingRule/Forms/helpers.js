import _ from 'lodash';
import { URI } from 'ecc-utils';

export const wasTouched = (initialValues, currentState) =>
    _.some(initialValues, (value, key) => value !== currentState[key]);

/** Tests if the value is a relative or absolute IRI or URN? */
export const newValueIsIRI = ({ label }) => {
    try {
        if (label.length > 0) {
            const uri = new URI(label.replace(/^<|>$/g, ''));
            return uri.is('resourceURI') || uri.is('url') && uri.is('relative');
        }
        return false;
    } catch (e) {
        // If the URI constructor throws an Error,
        // we can be sure that the entered string is not an URI
        return false;
    }
};

/** Converts a string to a normalized URI. Used when the user is expected to enter a valid (possibly relative) URI.
 * Usable in auto complete widget as newOptionCreator function. */
export const convertToUri = ({ label, labelKey, valueKey }) => {
    let value = label;
    const before = `${label} ${labelKey} ${valueKey}`;
    try {
        const regex = /^Create option "(.*)"$/;
        const match = regex.exec(label);
        if (match !== null && match.length > 1) {
            // Handle prompt, this is a hack, since newOptionCreator is also run on the prompt for some unknown reason
            const normalized = new URI(match[1]).normalize().toString();
            if (normalized !== match[1]) {
                value = `Normalizing URI to "${normalized}"`;
            }
        } else {
            value = new URI(label).normalize().toString();
        }
    } catch (e) {}

    return {
        [label]: value,
        [labelKey]: label,
        [valueKey]: value,
    };
};
