export const collectTransitiveDependentParameters = (
    changedParameterId: string,
    dependencyGraph: Map<string, Set<string>>,
): string[] => {
    const visited = new Set<string>([changedParameterId]);
    const collected: string[] = [];

    const collect = (currentParamId: string) => {
        const dependentParams = dependencyGraph.get(currentParamId) ?? [];
        dependentParams.forEach((paramId: string) => {
            if (!visited.has(paramId)) {
                visited.add(paramId);
                collected.push(paramId);
                collect(paramId);
            }
        });
    };

    collect(changedParameterId);
    return collected;
};

export const collectPopulatedDependentParameters = (
    changedParameterId: string,
    dependencyGraph: Map<string, Set<string>>,
    getValue: (paramId: string) => any,
): string[] => {
    return collectTransitiveDependentParameters(changedParameterId, dependencyGraph).filter((paramId) => {
        return paramId !== changedParameterId && !!getValue(paramId);
    });
};
