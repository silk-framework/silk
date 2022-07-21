import _ from 'lodash';

const getPathsRecursive = (operator = {}, accumulator = []) => {
    if (_.has(operator, 'path')) {
        accumulator.push(operator.path);
    }
    // @FIXME: why operator.function needed?
    if (_.has(operator, 'function') && _.has(operator, 'inputs')) {
        _.forEach(
            operator.inputs,
            input =>
                (accumulator = _.concat(
                    accumulator,
                    getPathsRecursive(input)
                ))
        );
    }
    return accumulator;
};

export default getPathsRecursive;
