import _ from "lodash";

const getPathsRecursive = (operator = {}, accumulator = []) => {
    if (_.has(operator, "path")) {
        accumulator.push(operator.path);
    }
    if (operator.type === "ruleBlockInput" && _.has(operator, "bindings")) {
        _.forEach(
            operator.bindings,
            (binding) => (accumulator = _.concat(accumulator, getPathsRecursive(binding.input))),
        );
    }
    // @FIXME: why operator.function needed?
    if (_.has(operator, "function") && _.has(operator, "inputs")) {
        _.forEach(operator.inputs, (input) => (accumulator = _.concat(accumulator, getPathsRecursive(input))));
    }
    return accumulator;
};

export default getPathsRecursive;
