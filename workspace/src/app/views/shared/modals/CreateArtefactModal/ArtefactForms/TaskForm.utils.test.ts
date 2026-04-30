import { collectPopulatedDependentParameters, collectTransitiveDependentParameters } from "./TaskForm.utils";

describe("TaskForm utils", () => {
    it("collects transitive dependent parameters", () => {
        const dependencyGraph = new Map<string, Set<string>>([
            ["connection.password", new Set(["connection.database", "connection.schema"])],
            ["connection.database", new Set(["connection.table"])],
        ]);

        expect(collectTransitiveDependentParameters("connection.password", dependencyGraph)).toEqual([
            "connection.database",
            "connection.table",
            "connection.schema",
        ]);
    });

    it("collects only populated dependent parameters and ignores the changed parameter in cycles", () => {
        const dependencyGraph = new Map<string, Set<string>>([
            ["connection.password", new Set(["connection.database", "connection.schema", "connection.role"])],
            ["connection.database", new Set(["connection.password", "connection.table"])],
        ]);
        const values: Record<string, any> = {
            "connection.password": "secret",
            "connection.database": "analytics",
            "connection.schema": "",
            "connection.role": undefined,
            "connection.table": "customers",
        };

        expect(
            collectPopulatedDependentParameters("connection.password", dependencyGraph, (paramId) => values[paramId]),
        ).toEqual(["connection.database", "connection.table"]);
    });
});
