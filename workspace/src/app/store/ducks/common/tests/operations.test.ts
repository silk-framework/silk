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

    test("extractParameterValues should remove the meta data fields and the dataset attributes", () => {
        const formValues = {
            label: "Some label",
            description: "Some description",
            id: "someId",
            tags: { selectedItems: [] },
            readOnly: true,
            uriProperty: "urn:uri",
            file: "some file",
            // Only root parameters are removed, nested parameters may have the same name
            objectParameter: { label: "nested label" },
        };
        expect(commonOps.extractParameterValues(formValues)).toStrictEqual({
            file: "some file",
            objectParameter: { label: "nested label" },
        });
    });
});
