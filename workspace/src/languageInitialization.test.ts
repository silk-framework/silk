describe("language initialization", () => {
    beforeEach(() => {
        localStorage.clear();
    });

    afterEach(() => {
        window.history.replaceState({}, "", "/");
        localStorage.clear();
    });

    test("keeps an explicit user choice ahead of URL detection", () => {
        localStorage.setItem("i18nUserChoice", "true");
        localStorage.setItem("i18nextLng", "de");
        window.history.replaceState({}, "", "/?lng=fr");

        jest.isolateModules(() => {
            const { default: i18n } = require("./language") as typeof import("./language");
            expect(i18n.language).toBe("de");
            expect(localStorage.getItem("i18nextLng")).toBe("de");
        });
    });

    test("normalizes a regional detected locale to a supported language", () => {
        window.history.replaceState({}, "", "/?lng=en-US");

        jest.isolateModules(() => {
            const { default: i18n } = require("./language") as typeof import("./language");
            expect(i18n.language).toBe("en");
        });
    });
});
