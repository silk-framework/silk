const fs = require("fs");
const path = require("path");
const { execFile } = require("child_process");
const { promisify } = require("util");

const runNode = promisify(execFile);
const writePackage = (directory, name, version) => {
    fs.mkdirSync(directory, { recursive: true });
    fs.writeFileSync(path.join(directory, "package.json"), JSON.stringify({ name, version, main: "index.js" }));
    fs.writeFileSync(path.join(directory, "index.js"), "module.exports = {};");
};

describe("shared companion library versions", () => {
    let directory;

    beforeEach(() => {
        const temporaryRoot = path.resolve(".local");
        fs.mkdirSync(temporaryRoot, { recursive: true });
        directory = fs.mkdtempSync(path.join(temporaryRoot, "shared-versions-"));
        writePackage(path.join(directory, "gui-checkout"), "@eccenca/gui-elements", "26.77.1");
        writePackage(path.join(directory, "node_modules/@eccenca/gui-elements"), "@eccenca/gui-elements", "26.1.0");
        writePackage(path.join(directory, "node_modules/@blueprintjs/core"), "@blueprintjs/core", "6.1.0");
        writePackage(path.join(directory, "src/node_modules/@blueprintjs/core"), "@blueprintjs/core", "6.77.1");
        fs.writeFileSync(path.join(directory, "src/index.js"), "module.exports = __COMPANION_SHARED_VERSIONS__;");
    });

    afterEach(() => {
        fs.rmSync(directory, { recursive: true, force: true });
    });

    const compile = async (mode, watch) => {
        // Run Webpack in Node, as the build scripts do, outside Jest's Babel transforms and jsdom.
        await runNode(
            process.execPath,
            [path.join(__dirname, "fixtures/sharedLibraryVersions.cjs"), directory, mode, String(watch)],
            {
                timeout: 20000,
            },
        );
        return JSON.parse(fs.readFileSync(path.join(directory, "versions.json"), "utf8"));
    };

    it.each([
        ["development", false],
        ["development", true],
        ["production", false],
    ])("injects the aliased checkout and importing module's packages in %s (watch=%s)", async (mode, watch) => {
        const [versions] = await compile(mode, watch);
        expect(versions).toEqual({ "@eccenca/gui-elements": "26.77.1", "@blueprintjs/core": "6.77.1" });
    });

    it("updates both versions when package metadata changes without restarting the watcher", async () => {
        const versions = await compile("development", true);
        expect(versions).toEqual([
            { "@eccenca/gui-elements": "26.77.1", "@blueprintjs/core": "6.77.1" },
            { "@eccenca/gui-elements": "26.77.2", "@blueprintjs/core": "6.77.1" },
            { "@eccenca/gui-elements": "26.77.2", "@blueprintjs/core": "6.77.2" },
        ]);
    });

    it("fails the build when a resolved library has no declared version", async () => {
        writePackage(path.join(directory, "gui-checkout"), "@eccenca/gui-elements", "");
        await expect(compile("development", false)).rejects.toMatchObject({ code: 1 });
        expect(JSON.parse(fs.readFileSync(path.join(directory, "failure.json"), "utf8"))).toEqual({
            message: expect.stringContaining("Missing version for shared library @eccenca/gui-elements"),
        });
    });
});
