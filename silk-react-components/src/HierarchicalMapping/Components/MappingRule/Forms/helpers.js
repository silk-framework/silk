
import _ from 'lodash';

export const wasTouched = (initialValues, currentState) => {

    return _.some(initialValues, (value, key) => {
        return value !== currentState[key];
    });

}
