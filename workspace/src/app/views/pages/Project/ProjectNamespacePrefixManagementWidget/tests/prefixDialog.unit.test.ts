import { validatePrefixName, validatePrefixValue } from "../PrefixNew";

describe("Add new prefix", () => {
    test("should validate prefix names", () => {
        const mustBeFalse = (value: string) => expect(validatePrefixName(value)).toBe(false);
        const mustBeTrue = (value: string) => expect(validatePrefixName(value)).toBe(true);
        mustBeTrue("foaf");
        mustBeTrue("a42");
        mustBeTrue("a.b.c");
        mustBeTrue("a_b");
        mustBeTrue("A-B-7");
        mustBeTrue("äß");
        // Invalid prefix names
        mustBeFalse("a b");
        mustBeFalse("");
        mustBeFalse("3");
        mustBeFalse("a.b.");
        mustBeFalse(" a");
        mustBeFalse("a ");
    });

    test("should validate prefix values", () => {
        const mustBeFalse = (value: string, expectedInvalidCharIdx?: number) =>
            expect(validatePrefixValue(value)).toBe(expectedInvalidCharIdx ?? false);
        const mustBeTrue = (value: string) => expect(validatePrefixValue(value)).toBe(true);
        mustBeTrue("http://eccenca.com");
        mustBeTrue("http://eccenca.com/path/");
        mustBeTrue("urn:test:");
        mustBeTrue("https://something.org/some/path#");
        // Invalid values
        mustBeFalse("localName");
        mustBeFalse("http://");
        mustBeFalse("urn:");
        mustBeFalse("https://hello.com/ some spaces ", 18);
        mustBeFalse(" urn:singleSpace", 0);
        mustBeFalse("urn:singleSpace ", 15);
        mustBeFalse("urn:singleTab\t", 13);
        mustBeFalse("");
        mustBeFalse(" ", 0);
        mustBeFalse("urn:someInvalidChars{}", 20);
    });
});
