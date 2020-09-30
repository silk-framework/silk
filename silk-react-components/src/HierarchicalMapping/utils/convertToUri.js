import { URI } from 'ecc-utils';

/** Converts a string to a normalized URI. Used when the user is expected to enter a valid (possibly relative) URI.
 * Usable in auto complete widget as newOptionCreator function. */
export const convertToUri = ({label, labelKey, valueKey}) => {
    let value = label;
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
    } catch (e) {
    }
    
    return {
        [label]: value,
        [labelKey]: label,
        [valueKey]: value,
    };
};
