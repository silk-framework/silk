const path = require("path");

const checkRequiredFiles = require("../../scripts/checkRequiredFiles");

describe("checkRequiredFiles", () => {
    let logSpy;

    beforeEach(() => {
        logSpy = jest.spyOn(console, "log").mockImplementation(() => undefined);
    });

    afterEach(() => {
        logSpy.mockRestore();
    });

    it("accepts files that exist", () => {
        expect(checkRequiredFiles([__filename])).toBe(true);
        expect(logSpy).not.toHaveBeenCalled();
    });

    it("reports the first missing file", () => {
        const missingFile = path.join(__dirname, "missing-entry.tsx");

        expect(checkRequiredFiles([__filename, missingFile])).toBe(false);
        expect(logSpy.mock.calls.flat().join("\n")).toContain("Could not find a required file.");
        expect(logSpy.mock.calls.flat().join("\n")).toContain("missing-entry.tsx");
        expect(logSpy.mock.calls.flat().join("\n")).toContain(__dirname);
    });
});
