'use strict';

// Do this as the first thing so that any code reading it knows the right env.
process.env.BABEL_ENV = 'development';
process.env.NODE_ENV = 'development';

// Makes the script crash on unhandled rejections instead of silently
// ignoring them. In the future, promise rejections that are not handled will
// terminate the Node.js process with a non-zero exit code.
process.on('unhandledRejection', err => {
    throw err;
});

// Ensure environment variables are read.
require('../config/env');

const path = require('path');
const chalk = require('react-dev-utils/chalk');
const fs = require('fs-extra');
const webpack = require('webpack');
const bfj = require('bfj');
const configFactory = require('../config/webpack.di.config');
const paths = require('../config/paths');
const checkRequiredFiles = require('react-dev-utils/checkRequiredFiles');
const formatWebpackMessages = require('react-dev-utils/formatWebpackMessages');
const printBuildError = require('react-dev-utils/printBuildError');
const ignoredFiles = require('react-dev-utils/ignoredFiles');

const isInteractive = process.stdout.isTTY;
// Warn and crash if required files are missing
if (!checkRequiredFiles([paths.appHtml, paths.appIndexJs])) {
    process.exit(1);
}

// Process CLI arguments
const argv = process.argv.slice(2);
const writeStatsJson = argv.indexOf('--stats') !== -1;
const isWatch = argv.indexOf('--watch') !== -1;
// Generate configuration
const config = configFactory('development');

// We require that you explicitly set browsers and do not fall back to
// browserslist defaults.
const {checkBrowsers} = require('react-dev-utils/browsersHelper');
checkBrowsers(paths.appPath, isInteractive)
    .then(
        () => {
            // Remove all content but keep the directory so that
            // if you're in it, you don't end up in Trash
            fs.emptyDirSync(paths.appDIBuild);
            // Merge with the public folder
            copyPublicFolder();
            // Start the webpack build
            run();
        }
    )
    .catch(err => {
        if (err && err.message) {
            console.log(err.message);
        }
        process.exit(1);
    });

function exitOnError(err) {
    console.log(chalk.red('Failed to compile.\n'));
    printBuildError(err);
    // process.exit(1);
}

function runCallback(err, stats) {
    let messages;
    if (err) {
        if (!err.message) {
            return exitOnError(err)
        }
        messages = formatWebpackMessages({
            errors: [err.message],
            warnings: [],
        });
    } else {
        messages = formatWebpackMessages(
            stats.toJson({
                all: false,
                warnings: true,
                errors: true,
            })
        );
    }
    if (messages.errors.length) {
        // Only keep the first error. Others are often indicative
        // of the same problem, but confuse the reader with noise.
        if (messages.errors.length > 1) {
            messages.errors.length = 1;
        }
        return exitOnError(new Error(messages.errors.join('\n\n')));
    }
    
    if (messages.warnings.length) {
        console.log(chalk.yellow('Compiled with warnings.\n'));
        console.log(messages.warnings.join('\n\n'));
    }
    
    fs.emptyDirSync(paths.appDIAssets + '/assets');
    // Copy assets into assets public folder
    copyAssetsToPublicFolder();
    console.log(chalk.green('Compiled successfully.\n'));
    
    if (writeStatsJson) {
        return bfj
            .write(paths.appDIBuild + '/bundle-stats.json', stats.toJson())
    }
    
    console.log('listening to new changes...');
}

// Create the production build and print the deployment instructions.
function run() {
    const compiler = webpack(config);
    if (isWatch) {
        return compiler.watch({
            aggregateTimeout: 300,
            watchOptions: {
                ignored: /node_modules/
            }
        }, runCallback);
    }
    compiler.run(runCallback)
}

function copyPublicFolder() {
    fs.copySync(paths.appPublic, paths.appDIBuild, {
        dereference: true,
        filter: file => file !== paths.appHtml,
    });
}

function copyAssetsToPublicFolder() {
    fs.copySync(
        path.join(paths.appDIBuild, 'assets'),
        path.join(paths.appDIAssets, 'assets')
    );
}
