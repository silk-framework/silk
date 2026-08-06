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

function parseBuildConfig(fileContent) {
    const configProperties = {};
    fileContent.split(/\r?\n/).forEach((rawLine) => {
        const line = rawLine.trim();
        if (!line || line.startsWith("#")) {
            return;
        }

        const separatorIndex = line.indexOf("=");
        if (separatorIndex <= 0) {
            return;
        }

        const key = line.slice(0, separatorIndex).trim();
        const value = line.slice(separatorIndex + 1).trim();
        if (key) {
            configProperties[key] = value;
        }
    });
    return configProperties;
}

function buildConfig() {
    const configFile = "silk-ui-build.properties";
    let directory = appDirectory;
    for (let count = 0; count < 3; count += 1) {
        const configPath = path.join(directory, configFile);
        try {
            const fileContent = fs.readFileSync(configPath, "utf8");
            console.log("Found Silk UI config file at " + configPath);
            return parseBuildConfig(fileContent);
        } catch (error) {
            if (error.code !== "ENOENT" && error.code !== "ENOTDIR") {
                throw error;
            }
        }
        directory = path.dirname(directory);
    }
    return {};
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

const silkConfig = buildConfig();

const configValue = (key, defaultValue) => {
    const value = silkConfig[key] ? silkConfig[key] : defaultValue;
    if (value !== defaultValue) {
        console.log(`Using non-default value for config key '${key}': '${value}'`);
    }
    return value;
};

const configuredPaths = (value) =>
    value
        ? value
              .split(";")
              .map((configuredPath) => configuredPath.trim())
              .filter(Boolean)
              .map(resolveApp)
        : [];

// Allow to add additional source paths, e.g. proprietary code that will be bundled together with the core code.
// Paths are separated by ';' and are relative to the 'workspace' folder.
const additionalSourcePaths = () => {
    const pathsString = silkConfig.additionalSources
        ? silkConfig.additionalSources
        : process.env.ADDITIONAL_SOURCE_PATHS;
    return configuredPaths(pathsString);
};

// Allow to add additional entry points, e.g. from proprietary code that will be bundled together with the core code.
// Entries are separated by ';' and are relative to the 'workspace' folder.
const additionalEntries = () => {
    const entriesString = silkConfig.additionalEntries ? silkConfig.additionalEntries : process.env.ADDITIONAL_ENTRIES;
    return configuredPaths(entriesString);
};

// config after eject: we're in ./config/
module.exports = {
    dotenv: resolveApp(".env"),
    appPath: resolveApp("."),
    appBuild: resolveApp("build"),
    watchDIBuild: resolveApp(configValue("watchDIBuild", "../silk-workbench/target/web/public/main")),
    watchDIAssets: resolveApp(
        configValue("watchDIAssets", "../silk-workbench/target/web/public/main/lib/silk-workbench-core/new-workspace"),
    ),
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
    additionalEntries,
};

module.exports.moduleFileExtensions = moduleFileExtensions;
