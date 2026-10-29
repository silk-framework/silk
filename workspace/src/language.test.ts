import { fetchUserSelectedLanguage, markLanguageAsUserChoice } from "./language";

describe("stored language choice", () => {
    beforeEach(() => {
        localStorage.clear();
    });

    test("does not treat a language cached by i18next as a user choice", () => {
        localStorage.setItem("i18nextLng", "de");

        expect(fetchUserSelectedLanguage()).toBeUndefined();
    });

    test("returns the language selected by a user", () => {
        localStorage.setItem("i18nUserChoice", "true");
        localStorage.setItem("i18nextLng", "fr");

        expect(fetchUserSelectedLanguage()).toBe("fr");
    });

    test("reads a language stored by older versions of DI", () => {
        localStorage.setItem("i18nUserChoice", "true");
        localStorage.setItem("i18nextLng", '"de"');

        expect(fetchUserSelectedLanguage()).toBe("de");
    });

    test("marks a user choice without rewriting the language cached by i18next", () => {
        localStorage.setItem("i18nextLng", "fr");

        markLanguageAsUserChoice();

        expect(localStorage.getItem("i18nextLng")).toBe("fr");
        expect(localStorage.getItem("i18nUserChoice")).toBe("true");
    });

    test("keeps language changes working when browser storage is unavailable", () => {
        const getItem = jest.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
            throw new Error("Storage unavailable");
        });
        expect(fetchUserSelectedLanguage()).toBeUndefined();
        getItem.mockRestore();

        const setItem = jest.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
            throw new Error("Storage unavailable");
        });
        expect(markLanguageAsUserChoice).not.toThrow();
        setItem.mockRestore();
    });
});
