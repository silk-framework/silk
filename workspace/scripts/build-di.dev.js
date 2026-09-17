"use strict";

process.env.BABEL_ENV = "development";
process.env.NODE_ENV = "development";

process.on("unhandledRejection", (error) => {
    throw error;
});

require("../config/env");

const path = require("path");
const chalk = require("react-dev-utils/chalk");
const fs = require("fs-extra");
const webpack = require("webpack");
const bfj = require("bfj");
const configFactory = require("../config/webpack.di.config");
const paths = require("../config/paths");
const utils = require("./build.utils");
const checkRequiredFiles = require("./checkRequiredFiles");
const formatWebpackMessages = require("react-dev-utils/formatWebpackMessages");
const printBuildError = require("react-dev-utils/printBuildError");

const argv = process.argv.slice(2);
const writeStatsJson = argv.includes("--stats") || argv.includes("--profile");
const isWatch = argv.includes("--watch");
const diagnostics = {
    analyze: argv.includes("--analyze"),
    profile: argv.includes("--profile"),
};
const config = configFactory("development", isWatch, diagnostics);
const diBuildPath = isWatch ? paths.watchDIBuild : paths.appDIBuild;
const diAssetsPath = isWatch ? paths.watchDIAssets : paths.appDIAssets;
const isInteractive = process.stdout.isTTY;
let watcher;

if (!checkRequiredFiles([paths.appHtml, paths.appIndexJs, paths.stylesEntry])) {
    process.exitCode = 1;
} else {
    const { checkBrowsers } = require("react-dev-utils/browsersHelper");
    checkBrowsers(paths.appPath, isInteractive)
        .then(() => {
            if (!isWatch) {
                fs.emptyDirSync(diBuildPath);
            }
            copyPublicFolder();
            return run();
        })
        .catch(failFatally);
}

function failFatally(error) {
    console.log(chalk.red("Failed to compile.\n"));
    printBuildError(error);
    process.exitCode = 1;
}

function reportCompilationFailure(error) {
    console.log(chalk.red("Failed to compile.\n"));
    printBuildError(error);
    utils.setBuildFailureExitCode(isWatch);
}

async function handleCompilation(error, stats) {
    let messages;
    if (error) {
        messages = formatWebpackMessages({
            errors: [error.message || String(error)],
            warnings: [],
        });
    } else {
        messages = formatWebpackMessages(stats.toJson({ all: false, warnings: true, errors: true }));
    }

    if (messages.warnings.length) {
        console.log(messages.warnings.join("\n"));
        console.log(chalk.yellow("Compiled with warnings.\n"));
    }

    if (messages.errors.length) {
        reportCompilationFailure(new Error(messages.errors[0]));
        return;
    }

    if (!messages.warnings.length) {
        console.log(chalk.green("Compiled successfully.\n"));
    }

    copyCurrentAssetsToPublicFolder(stats);

    if (writeStatsJson) {
        await bfj.write(path.join(diBuildPath, "bundle-stats.json"), stats.toJson());
    }

    if (isWatch) {
        console.log("Listening for changes...");
    }
}

function run() {
    const compiler = webpack(config);
    if (isWatch) {
        watcher = compiler.watch(
            {
                aggregateTimeout: 300,
                ignored: /node_modules/,
            },
            (error, stats) => {
                handleCompilation(error, stats).catch(reportCompilationFailure);
            },
        );
        installWatchSignalHandlers();
        return watcher;
    }

    return utils.runCompiler(compiler).then((stats) => handleCompilation(null, stats));
}

function installWatchSignalHandlers() {
    const closeWatcher = () => {
        watcher.close((error) => {
            if (error) {
                printBuildError(error);
                process.exitCode = 1;
            }
        });
    };
    process.once("SIGINT", closeWatcher);
    process.once("SIGTERM", closeWatcher);
}

function copyPublicFolder() {
    fs.copySync(paths.appPublic, diBuildPath, {
        dereference: true,
        filter: (file) => file !== paths.appHtml,
    });
}

function copyCurrentAssetsToPublicFolder(stats) {
    const destinationAssetsPath = path.join(diAssetsPath, "assets");
    const assetNames = stats.compilation
        .getAssets()
        .map((asset) => asset.name)
        .filter((assetName) => assetName.startsWith("assets/"));

    fs.emptyDirSync(destinationAssetsPath);
    assetNames.forEach((assetName) => {
        const sourcePath = path.join(diBuildPath, assetName);
        if (fs.existsSync(sourcePath)) {
            fs.copySync(sourcePath, path.join(diAssetsPath, assetName));
        }
    });
}
