import _ from 'lodash';

export const wasTouched = (initialValues, currentState) =>
    _.some(initialValues, (value, key) => value !== currentState[key]);
