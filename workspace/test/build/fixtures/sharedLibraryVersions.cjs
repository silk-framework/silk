process.env.BABEL_ENV = "development";
process.env.NODE_ENV = "development";

const fs = require("fs");
const path = require("path");
const vm = require("vm");
const webpack = require("webpack");
const configFactory = require("../../../config/webpack.di.config");

const [directory, mode, watchArgument] = process.argv.slice(2);
const watch = watchArgument === "true";
const config = configFactory(mode, watch);
const versionsPlugin = config.plugins.find((plugin) => plugin.constructor.name === "SharedLibraryVersionsPlugin");
if (!versionsPlugin) throw new Error("Shared library version injection is missing from the build configuration");

const compiler = webpack({
    mode,
    context: directory,
    entry: "./src/index.js",
    output: { path: path.join(directory, "output"), filename: "bundle.js", library: { type: "commonjs2" } },
    resolve: {
        alias: {
            "@eccenca/gui-elements$": path.join(directory, "gui-checkout/index.js"),
            "@eccenca/gui-elements": path.join(directory, "gui-checkout"),
        },
    },
    plugins: [versionsPlugin],
});

const versions = [];
let watcher;
const fail = (error) => {
    if (error) {
        console.error(error);
        process.exitCode = 1;
    }
};
const close = () => {
    if (watcher)
        watcher.close((error) => {
            fail(error);
            compiler.close(fail);
        });
    else compiler.close(fail);
};
const onBuild = (error, stats) => {
    try {
        if (error) throw error;
        if (stats.hasErrors()) throw new Error(stats.toString({ all: false, errors: true }));
        const sandbox = { module: { exports: {} } };
        vm.runInNewContext(fs.readFileSync(path.join(directory, "output/bundle.js"), "utf8"), sandbox);
        versions.push(sandbox.module.exports);
        if (!watch || versions.length === 3) {
            fs.writeFileSync(path.join(directory, "versions.json"), JSON.stringify(versions));
            close();
        } else if (versions.length === 1) {
            fs.writeFileSync(
                path.join(directory, "gui-checkout/package.json"),
                JSON.stringify({ name: "@eccenca/gui-elements", version: "26.77.2" }),
            );
        } else {
            fs.writeFileSync(
                path.join(directory, "src/node_modules/@blueprintjs/core/package.json"),
                JSON.stringify({ name: "@blueprintjs/core", version: "6.77.2" }),
            );
        }
    } catch (failure) {
        fs.writeFileSync(path.join(directory, "failure.json"), JSON.stringify({ message: String(failure) }));
        fail(failure);
        close();
    }
};
if (watch) watcher = compiler.watch({ ...config.watchOptions, aggregateTimeout: 20 }, onBuild);
else compiler.run(onBuild);
