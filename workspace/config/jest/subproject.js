/* import type {Config} from "jest"; */
const fs = require("fs");
const path = require("path");

const orgconfig = require("./config.js");

const rootDir = path.resolve(__dirname, orgconfig.rootDir);
// Support both standalone Silk checkouts and Silk used as a sub-module in a parent repo.
const nodeModulesDirCandidates = [
    path.resolve(rootDir, "../../node_modules"),
    path.resolve(rootDir, "../node_modules"),
];

const nodeModulesDir =
    nodeModulesDirCandidates.find((candidate) =>
        fs.existsSync(path.join(candidate, "@eccenca", "gui-elements", "package.json")),
    ) || path.resolve(rootDir, "../node_modules");

const mapFromNodeModules = (packagePath) => path.join(nodeModulesDir, packagePath);

module.exports = {
    ...orgconfig,
    moduleNameMapper: {
        ...orgconfig.moduleNameMapper,
        "^react-markdown$": mapFromNodeModules("react-markdown"),
        "^@eccenca/gui-elements$": mapFromNodeModules("@eccenca/gui-elements"),
        "^@eccenca/gui-elements/(.*)$": `${mapFromNodeModules("@eccenca/gui-elements")}/$1`,
        "^@reduxjs/toolkit$": mapFromNodeModules("@reduxjs/toolkit/dist/cjs/redux-toolkit.development.cjs"),
    },
};
