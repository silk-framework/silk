"use strict";

// Do this as the first thing so that any code reading it knows the right env.
process.env.BABEL_ENV = "production";
process.env.NODE_ENV = "production";

// Makes the script crash on unhandled rejections instead of silently
// ignoring them. In the future, promise rejections that are not handled will
// terminate the Node.js process with a non-zero exit code.
process.on("unhandledRejection", (err) => {
    throw err;
});

// Ensure environment variables are read.
require("../config/env");

const path = require("path");
const chalk = require("react-dev-utils/chalk");
const fs = require("fs-extra");
const webpack = require("webpack");
const bfj = require("bfj");
const configFactory = require("../config/webpack.di.config");
const paths = require("../config/paths");
const checkRequiredFiles = require("react-dev-utils/checkRequiredFiles");
const formatWebpackMessages = require("react-dev-utils/formatWebpackMessages");
const printHostingInstructions = require("react-dev-utils/printHostingInstructions");
const FileSizeReporter = require("react-dev-utils/FileSizeReporter");
const printBuildError = require("react-dev-utils/printBuildError");

const measureFileSizesBeforeBuild = FileSizeReporter.measureFileSizesBeforeBuild;
const printFileSizesAfterBuild = FileSizeReporter.printFileSizesAfterBuild;
const useYarn = fs.existsSync(paths.yarnLockFile);

// These sizes are pretty large. We'll warn for bundles exceeding them.
const WARN_AFTER_BUNDLE_GZIP_SIZE = 512 * 1024;
const WARN_AFTER_CHUNK_GZIP_SIZE = 1024 * 1024;

const isInteractive = process.stdout.isTTY;

// Warn and crash if required files are missing
if (!checkRequiredFiles([paths.appHtml, paths.appIndexJs])) {
    process.exit(1);
}

// Process CLI arguments
const argv = process.argv.slice(2);
const writeStatsJson = argv.indexOf("--stats") !== -1;

// Generate configuration
const config = configFactory("production");

function logSpentTime() {
    const timers = [];
    return {
        startLog: (label) => {
            timers.push(label);
            console.time(chalk.blue(label));
        },
        stopLog: (label) => {
            const ind = timers.indexOf(label);
            if (ind > -1) {
                timers.splice(0, ind);
                return console.timeEnd(chalk.blue(label));
            }
        },
    };
}

// We require that you explicitly set browsers and do not fall back to
// browserslist defaults.
const { checkBrowsers } = require("react-dev-utils/browsersHelper");
const utils = require("./build.utils");
const { startLog, stopLog } = logSpentTime();

checkBrowsers(paths.appPath, isInteractive)
    .then(() => {
        // First, read the current file sizes in build directory.
        // This lets us display how much they changed later.
        startLog("Time for file size measurement:");
        return measureFileSizesBeforeBuild(paths.appDIBuild);
    })
    .then((previousFileSizes) => {
        stopLog("Time for file size measurement:");
        // Remove all content but keep the directory so that
        // if you're in it, you don't end up in Trash
        startLog("Cleared old build folder:");
        fs.emptyDirSync(paths.appDIBuild);
        stopLog("Cleared old build folder:");

        // Merge with the public folder
        copyPublicFolder();

        // Start the webpack build
        console.log(chalk.white("Creating a production build..."));
        return build(previousFileSizes);
    })
    .then(
        ({ stats, previousFileSizes, warnings }) => {
            if (warnings.length) {
                console.log(chalk.yellow("Compiled with warnings.\n"));
                console.log(warnings.join("\n\n"));
            }
            console.log(chalk.green("Compiled successfully.\n"));
            console.log("File sizes after gzip:\n");
            printFileSizesAfterBuild(
                stats,
                previousFileSizes,
                paths.appDIBuild,
                WARN_AFTER_BUNDLE_GZIP_SIZE,
                WARN_AFTER_CHUNK_GZIP_SIZE
            );
            console.log();

            copyAssetsToPublicFolder();
        },
        (err) => {
            console.log(chalk.red("Failed to compile.\n"));
            printBuildError(err);
            process.exit(1);
        }
    )
    .catch((err) => {
        if (err && err.message) {
            console.log(err.message);
        }
        process.exit(1);
    })
    .finally(() => {
        stopLog("Production built ready:");
    });

// Create the production build and print the deployment instructions.
function build(previousFileSizes) {
    const adaptedConfig = utils.adaptWebpackConfig(config);
    const compiler = webpack(adaptedConfig);

    return new Promise((resolve, reject) => {
        compiler.run((err, stats) => {
            let messages;
            if (err) {
                if (!err.message) {
                    return reject(err);
                }
                messages = formatWebpackMessages({
                    errors: [err.message],
                    warnings: [],
                });
            } else {
                messages = formatWebpackMessages(stats.toJson({ all: false, warnings: true, errors: true }));
            }
            if (messages.errors.length) {
                // Only keep the first error. Others are often indicative
                // of the same problem, but confuse the reader with noise.
                if (messages.errors.length > 1) {
                    messages.errors.length = 1;
                }
                return reject(new Error(messages.errors.join("\n\n")));
            }
            if (
                process.env.CI &&
                (typeof process.env.CI !== "string" || process.env.CI.toLowerCase() !== "false") &&
                messages.warnings.length
            ) {
                // Disabled: Do not fail build because of warnings, e.g. linter warnings
                // console.log(
                //     chalk.yellow(
                //         "\nTreating warnings as errors because process.env.CI = true.\n" +
                //             "Most CI servers set it automatically.\n"
                //     )
                // );
                // return reject(new Error(messages.warnings.join("\n\n")));
            }

            const resolveArgs = {
                stats,
                previousFileSizes,
                warnings: messages.warnings,
            };
            if (writeStatsJson) {
                startLog("Generated bundle-stat.json");
                return bfj
                    .write(paths.appDIBuild + "/bundle-stats.json", stats.toJson())
                    .then(() => resolve(resolveArgs))
                    .catch((error) => reject(new Error(error)))
                    .finally(() => {
                        stopLog("Generated bundle-stat.json:");
                    });
            }

            return resolve(resolveArgs);
        });
    });
}

function copyPublicFolder() {
    startLog("Copied public folder:");
    fs.copySync(paths.appPublic, paths.appDIBuild, {
        dereference: true,
        filter: (file) => file !== paths.appHtml,
    });
    stopLog("Copied public folder:");
}

function copyAssetsToPublicFolder() {
    startLog("Cleared and copied assets folder to DI assets:");
    fs.emptyDirSync(paths.appDIAssets);
    const from = path.join(paths.appDIBuild, "assets");
    const to = path.join(paths.appDIAssets, "assets");
    console.log(`Copying assets from '${from}' to '${to}'.`);
    fs.copySync(from, to);
    stopLog("Cleared and copied assets folder to DI assets:");
}
