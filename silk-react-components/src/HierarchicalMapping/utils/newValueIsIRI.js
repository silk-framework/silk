import { URI } from 'ecc-utils';

/** Tests if the value is a relative or absolute IRI or URN? */
export const newValueIsIRI = ({label}) => {
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
