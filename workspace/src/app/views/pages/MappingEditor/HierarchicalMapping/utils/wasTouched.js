import _ from 'lodash';

export const wasTouched = (initialValues, currentState) => {
    return _.some(initialValues, (value, key) => {
        const currentValue = currentState[key]
        if(typeof value === "object" && typeof currentValue === "object") {
            return !_.isEqual(value, currentValue)
        } else {
            return value !== currentState[key]
        }
    });
}
