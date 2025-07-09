import { extractProperty } from "../PathInputOperator";

describe("extractProperty", () => {
    it("should extract the last property from complex path expressions", () => {
        ["foaf:name", "<http://some.prop/path>"].forEach((propToExtract) => {
            expect(extractProperty(propToExtract)).toBe(propToExtract);
            expect(extractProperty(`${propToExtract}[@lang = en]`)).toBe(propToExtract);
            expect(extractProperty(`${propToExtract}[<urn:prop:propB> = "some value"]`)).toBe(propToExtract);
            expect(extractProperty(`${propToExtract}[rdf:type != "some value"]`)).toBe(propToExtract);
            expect(extractProperty(`${propToExtract}[rdf:type = "some [ value"]`)).toBe(propToExtract);
            expect(extractProperty(`<http://some.prop/path>/${propToExtract}[@lang = en]`)).toBe(propToExtract);
            expect(extractProperty(`<http://some.prop/path>\\${propToExtract}[@lang = en]`)).toBe(propToExtract);
            expect(
                extractProperty(
                    `<http://some.prop/path>[<urn:prop:propA> = "some value"]/${propToExtract}[@lang = en]`,
                ),
            ).toBe(propToExtract);
            expect(extractProperty(`other:object[@lang = en]${propToExtract}`)).toBe(propToExtract);
        });
    });
});
