
import _ from 'lodash';
import {URI} from 'ecc-utils';

export const wasTouched = (initialValues, currentState) => {

    return _.some(initialValues, (value, key) => {
        return value !== currentState[key];
    });

};

export const newValueIsIRI = ({label}) => {

    if (_.isString(label)) {
        return (new URI(label)).is('resourceURI');
    }

    return false;

};