import _ from 'lodash';
import {URI} from 'ecc-utils';

export const wasTouched = (initialValues, currentState) =>
    _.some(initialValues, (value, key) => value !== currentState[key]);

export const newValueIsIRI = ({label}) => {
    try {
        return new URI(label.replace(/^<|>$/g, '')).is('resourceURI');
    } catch (e) {
        // If the URI constructor throws an Error,
        // we can be sure that the entered string is not an URI
    }

    return false;
};
