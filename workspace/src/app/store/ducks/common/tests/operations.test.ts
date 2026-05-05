import commonOps from "../operations";

describe("commonOps", () => {
    test("buildStringValuedObject should convert all literal values to string values in a nested object", () => {
        const flatObject = {
            id: 1,
            root: true,
            source: {
                id: 2,
                name: "2",
                extra: {
                    id: 4,
                    name: "extra",
                },
            },
            target: {
                id: 3,
                name: "3",
            },
        };
        const expectedResult = {
            id: "1",
            root: "true",
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
        expect(commonOps.buildStringValuedObject(flatObject)).toEqual(expectedResult);
    });
});
