const fs = require("fs");
const path = require("path");

const sourceRoot = path.resolve(__dirname, "../../src");
const sourceExtensions = new Set([".js", ".jsx", ".ts", ".tsx"]);

const sourceFilesBelow = (directory) =>
    fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
        const entryPath = path.join(directory, entry.name);
        if (entry.isDirectory()) {
            return sourceFilesBelow(entryPath);
        }
        return sourceExtensions.has(path.extname(entry.name)) ? [entryPath] : [];
    });

describe("standalone Silk source imports", () => {
    it("does not address Workspace through a DI monorepo path", () => {
        const invalidImports = sourceFilesBelow(sourceRoot).flatMap((file) => {
            const relativeFile = path.relative(sourceRoot, file);
            return fs
                .readFileSync(file, "utf8")
                .split("\n")
                .map((line, index) => ({ line: index + 1, relativeFile, source: line.trim() }))
                .filter(({ source }) => /["'](?:\.\.\/)+silk\/workspace\/src\//.test(source));
        });

        expect(invalidImports).toEqual([]);
    });
});
