"use strict";

const path = require("path");
const fs = require("fs");
const url = require("url");

// Make sure any symlinks in the project folder are resolved:
// https://github.com/facebook/create-react-app/issues/637
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

const envPublicUrl = process.env.PUBLIC_URL;

function ensureSlash(inputPath, needsSlash) {
    const hasSlash = inputPath.endsWith("/");
    if (hasSlash && !needsSlash) {
        return inputPath.substr(0, inputPath.length - 1);
    } else if (!hasSlash && needsSlash) {
        return `${inputPath}/`;
    } else {
        return inputPath;
    }
}

function buildConfig() {
    // Search for build config file "silk-ui-build.properties"
    const configFile = "silk-ui-build.properties"
    let configProperties = {}
    let dir = ""
    for(let count = 0; count < 3; count += 1) {
        try {
            const configPath = dir + configFile
            dir = dir + "../"
            const fileContent = fs.readFileSync(configPath).toString("utf-8")
            console.log("Found Silk UI config file at " + configPath)
            const lines = fileContent.split("\n")
            lines
                .map(p => p.trim().split("="))
                .filter(p => p.length > 1 && !p[0].startsWith("#"))
                .forEach(([p, v]) => {
                    configProperties[p] = v
                })
            dir = dir + "../"
        } catch(ex) {
            // ignore errors
        }
    }
    return configProperties
}

const getPublicUrl = (appPackageJson) => envPublicUrl || require(appPackageJson).homepage;

// We use `PUBLIC_URL` environment variable or "homepage" field to infer
// "public path" at which the app is served.
// Webpack needs to know it to put the right <script> hrefs into HTML even in
// single-page apps that may serve index.html for nested URLs like /todos/42.
// We can't use a relative path in HTML because we don't want to load something
// like /todos/42/static/js/bundle.7289d.js. We have to know the root.
function getServedPath(appPackageJson) {
    const publicUrl = getPublicUrl(appPackageJson);
    const servedUrl = envPublicUrl || (publicUrl ? url.parse(publicUrl).pathname : "/");
    return ensureSlash(servedUrl, true);
}

const moduleFileExtensions = [
    "web.mjs",
    "mjs",
    "web.js",
    "js",
    "web.ts",
    "ts",
    "web.tsx",
    "tsx",
    "json",
    "web.jsx",
    "jsx",
];

// Resolve file paths in the same order as webpack
const resolveModule = (resolveFn, filePath) => {
    const extension = moduleFileExtensions.find((extension) => fs.existsSync(resolveFn(`${filePath}.${extension}`)));

    if (extension) {
        return resolveFn(`${filePath}.${extension}`);
    }

    return resolveFn(`${filePath}.js`);
};

const silkConfig = buildConfig()

const configValue = (key, defaultValue) => silkConfig[key] ? silkConfig[key] : defaultValue

// Allow to add additional source paths, e.g. proprietary code that will be bundled together with the core code.
// Paths are separated by ';' and are relative to the 'workspace' folder.
const additionalSourcePaths = () => {
    const pathsString = silkConfig.additionalSources ? silkConfig.additionalSources : process.env.ADDITIONAL_SOURCE_PATHS
    if(pathsString) {
        return pathsString.split(";")
            .map(path => resolveApp(path))
    } else {
        return []
    }
}

// Allow to add additional entry points, e.g. from proprietary code that will be bundled together with the core code.
// Entries are separated by ';' and are relative to the 'workspace' folder.
const additionalEntries = () => {
    const entriesString = silkConfig.additionalEntries ? silkConfig.additionalEntries : process.env.ADDITIONAL_ENTRIES
    if(entriesString) {
        return entriesString.split(";")
            .map(path => resolveApp(path))
    } else {
        return []
    }
}

// config after eject: we're in ./config/
module.exports = {
    dotenv: resolveApp(".env"),
    appPath: resolveApp("."),
    appBuild: resolveApp("build"),
    watchDIBuild: resolveApp(configValue("watchDIBuild", "../silk-workbench/target/web/public/main")),
    watchDIAssets: resolveApp(configValue("watchDIAssets", "../silk-workbench/target/web/public/main/lib/silk-workbench-core/new-workspace")),
    appDIBuild: resolveApp(configValue("appDIBuild", "../silk-workbench/public")),
    appDIAssets: resolveApp("../silk-workbench/silk-workbench-core/public/new-workspace"),
    appDIAssetsUrl: "/core/assets/new-workspace/",
    appPublic: resolveApp("public"),
    appHtml: resolveApp("public/index.html"),
    appIndexJs: resolveModule(resolveApp, "src/index"),
    appPackageJson: resolveApp("package.json"),
    appSrc: resolveApp("src"),
    appTsConfig: resolveApp("tsconfig.json"),
    yarnLockFile: resolveApp("yarn.lock"),
    proxySetup: resolveApp("src/setupProxy.js"),
    appNodeModules: resolveApp("node_modules"),
    publicUrl: getPublicUrl(resolveApp("package.json")),
    servedPath: getServedPath(resolveApp("package.json")),
    ducksFolder: resolveApp("src/app/store/ducks"),
    guiElements: resolveApp("../libs/gui-elements"),
    silkConfig,
    additionalSourcePaths,
    additionalEntries
};

module.exports.moduleFileExtensions = moduleFileExtensions;
