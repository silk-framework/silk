const countRuleBlocksRecursive = (operator) => {
    if (!operator || typeof operator !== "object") {
        return 0;
    }
    if (operator.type === "ruleBlockInput") {
        return (
            1 +
            (operator.bindings ?? []).reduce(
                (count, binding) => count + countRuleBlocksRecursive(binding?.input),
                0,
            )
        );
    }
    if (Array.isArray(operator.inputs)) {
        return operator.inputs.reduce((count, input) => count + countRuleBlocksRecursive(input), 0);
    }
    return 0;
};

export default countRuleBlocksRecursive;
