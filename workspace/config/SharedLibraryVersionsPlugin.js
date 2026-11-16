const fs = require("fs");
const path = require("path");
const webpack = require("webpack");

const sharedLibraries = ["@eccenca/gui-elements", "@blueprintjs/core"];

const readPackageVersion = (entry, packageName, fileDependencies) => {
    let directory = path.dirname(entry);
    while (true) {
        const packageFile = path.join(directory, "package.json");
        if (fs.existsSync(packageFile)) {
            // Track metadata even if invalid, so correcting it can recover a watch build.
            fileDependencies.add(packageFile);
            const metadata = JSON.parse(fs.readFileSync(packageFile, "utf8"));
            if (metadata.name === packageName) {
                if (typeof metadata.version !== "string" || !metadata.version.trim()) {
                    throw new Error(`Missing version for shared library ${packageName} in ${packageFile}`);
                }
                return metadata.version;
            }
        }
        const parent = path.dirname(directory);
        if (parent === directory) {
            throw new Error(`Cannot find package metadata for shared library ${packageName} resolved to ${entry}`);
        }
        directory = parent;
    }
};

class SharedLibraryVersionsPlugin {
    apply(compiler) {
        new webpack.DefinePlugin({
            __COMPANION_SHARED_VERSIONS__: webpack.DefinePlugin.runtimeValue(({ module }) => {
                // Use the importing module's resolution, including aliases and nested installations.
                const resolver = compiler.resolverFactory.get("normal", {
                    dependencyType: "esm",
                    useSyncFileSystemCalls: true,
                    // DefinePlugin evaluates synchronously; Webpack's resolver cache is asynchronous.
                    cache: false,
                });
                const versions = {};
                for (const packageName of sharedLibraries) {
                    const entry = resolver.resolveSync({}, module.context, packageName);
                    if (!entry) throw new Error(`Cannot resolve shared library ${packageName} from ${module.context}`);
                    versions[packageName] = readPackageVersion(entry, packageName, module.buildInfo.fileDependencies);
                }
                return JSON.stringify(versions);
            }),
        }).apply(compiler);
    }
}

module.exports = SharedLibraryVersionsPlugin;
