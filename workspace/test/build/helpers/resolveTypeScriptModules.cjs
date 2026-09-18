// Run outside Jest's browser/transform environment; this checks the actual Node TypeScript resolver.
const path = require("path");
const [configFile, built] = process.argv.slice(2);
const root = path.dirname(configFile);
const ts = require(require.resolve("typescript", { paths: [root] }));
const config = ts.readConfigFile(configFile, ts.sys.readFile);
const parsed = ts.parseJsonConfigFileContent(config.config, ts.sys, root);
const errors = [config.error, ...parsed.errors].filter(Boolean);
if (errors.length)
    throw new Error(errors.map((error) => ts.flattenDiagnosticMessageText(error.messageText, "\n")).join("\n"));
const host = {
    ...ts.sys,
    directoryExists: (directory) => {
        const normalized = directory.replace(/\\/g, "/");
        if (/\/gui-elements\/dist(?:\/|$)/.test(normalized)) return built === "true";
        return ts.sys.directoryExists(directory);
    },
    fileExists: (file) => {
        // Match both the submodule path and its node_modules symlink before realpath resolution.
        const normalized = file.replace(/\\/g, "/");
        if (normalized.includes("/gui-elements/dist/")) {
            if (built === "false") return false;
            if (normalized.endsWith("/dist/types/index.d.ts")) return true;
        }
        return ts.sys.fileExists(file);
    },
};
const modules = [
    "@eccenca/gui-elements",
    "@eccenca/gui-elements/src/extensions/react-flow/versionsupport",
    "@ducks/workspace",
];
const resolved = Object.fromEntries(
    modules.map((name) => [
        name,
        ts.resolveModuleName(name, path.join(root, "src/index.ts"), parsed.options, host).resolvedModule
            ?.resolvedFileName,
    ]),
);
const uploadSource = path.join(
    path.dirname(resolved["@eccenca/gui-elements"]),
    "src/components/FileUpload/FileUpload.tsx",
);
for (const name of ["@uppy/core", "@uppy/react", "@uppy/xhr-upload"]) {
    resolved[name] = ts.resolveModuleName(name, uploadSource, parsed.options, host).resolvedModule?.resolvedFileName;
}
process.stdout.write(JSON.stringify(resolved));
