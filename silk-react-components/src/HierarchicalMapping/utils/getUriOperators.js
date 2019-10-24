import _ from 'lodash';

const getUriOperatorsRecursive = (operator = [], accumulator = []) => {
    if (_.has(operator, 'function')) {
        if (_.has(operator, 'inputs')) {
            _.forEach(
                operator.inputs,
                input =>
                    (accumulator = _.concat(
                        accumulator,
                        getUriOperatorsRecursive(input, [])
                    ))
            );
        }
        accumulator.push(operator.function);
    }
    
    return accumulator;
};

export default getUriOperatorsRecursive;
