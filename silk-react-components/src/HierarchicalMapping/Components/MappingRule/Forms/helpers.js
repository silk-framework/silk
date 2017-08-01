import _ from 'lodash';
import {URI} from 'ecc-utils';

export const wasTouched = (initialValues, currentState) =>
    _.some(initialValues, (value, key) => value !== currentState[key]);

export const newValueIsIRI = ({label}) => {
    if (_.isString(label)) {
        return new URI(label.replace(/^<|>$/g, '')).is('resourceURI');
    }

    return false;
};
