import _ from "lodash";

const getUriOperatorsRecursive = (operator = {}, accumulator = []) => {
    if (operator.type === "ruleBlockInput" && _.has(operator, "bindings")) {
        _.forEach(
            operator.bindings,
            (binding) => (accumulator = _.concat(accumulator, getUriOperatorsRecursive(binding.input))),
        );
    }
    if (_.has(operator, "function")) {
        if (_.has(operator, "inputs")) {
            _.forEach(
                operator.inputs,
                (input) => (accumulator = _.concat(accumulator, getUriOperatorsRecursive(input))),
            );
        }
        accumulator.push(operator.function);
    }

    return accumulator;
};

export default getUriOperatorsRecursive;
