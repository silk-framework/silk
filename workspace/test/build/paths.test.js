const fs = require("fs");
const path = require("path");

const configFileName = "silk-ui-build.properties";
const originalReadFileSync = fs.readFileSync.bind(fs);

const loadPathsWithConfigs = (...contents) => {
    let configReadIndex = 0;
    const readFile = jest.spyOn(fs, "readFileSync").mockImplementation((file, ...args) => {
        if (path.basename(String(file)) === configFileName) {
            if (configReadIndex < contents.length) {
                const content = contents[configReadIndex];
                configReadIndex += 1;
                return content;
            }
            const error = new Error(`No config at ${file}`);
            error.code = "ENOENT";
            throw error;
        }
        return originalReadFileSync(file, ...args);
    });

    let paths;
    jest.isolateModules(() => {
        paths = require("../../config/paths");
    });
    readFile.mockRestore();
    return paths;
};

describe("Silk UI build properties", () => {
    it("preserves property values and normalizes configured path lists", () => {
        const paths = loadPathsWithConfigs(`
            # build extensions
            malformed line
            opaqueValue=value=with=equals
            additionalEntries= src/index.tsx ; ; test/extra.tsx
            additionalSources= src ; test
        `);

        expect(paths.silkConfig.opaqueValue).toBe("value=with=equals");
        expect(paths.additionalEntries()).toEqual([path.resolve("src/index.tsx"), path.resolve("test/extra.tsx")]);
        expect(paths.additionalSourcePaths()).toEqual([path.resolve("src"), path.resolve("test")]);
    });

    it("uses the nearest properties file without merging an ancestor", () => {
        const paths = loadPathsWithConfigs("selected=nearest", "selected=ancestor\nancestorOnly=true");

        expect(paths.silkConfig).toEqual({ selected: "nearest" });
    });
});
