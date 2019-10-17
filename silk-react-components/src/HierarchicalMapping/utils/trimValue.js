import _ from 'lodash';

export const trimValue = arg => {
    if (_.isObject(arg)) {
        if (_.has(arg, 'value') && _.isString(arg.value)) {
            arg.value = _.trim(arg.value);
        }
        if (_.has(arg, 'label')) {
            arg.label = _.trim(arg.label);
        }
    } else if (_.isString(arg)) {
        return _.trim(arg);
    }
    
    return arg;
};
