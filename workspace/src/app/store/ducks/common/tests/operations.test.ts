import commonOps from "../operations";

describe("commonOps", () => {
    test("buildTaskObject should construct nested objects from flat objects", () => {
        const flatObject = {
            id: 1,
            "source.id": 2,
            "source.name": "2",
            "target.id": 3,
            "target.name": "3",
            "source.extra.id": 4,
            "source.extra.name": "extra",
        };
        const expectedResult = {
            id: "1",
            source: {
                id: "2",
                name: "2",
                extra: {
                    id: "4",
                    name: "extra",
                },
            },
            target: {
                id: "3",
                name: "3",
            },
        };
        expect(commonOps.buildTaskObject(flatObject)).toEqual(expectedResult);
    });
});
