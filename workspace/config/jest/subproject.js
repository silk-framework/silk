/* import type {Config} from "jest"; */
const orgconfig = require("./config.js");

module.exports = {
    ...orgconfig,
    moduleNameMapper: {
        ...orgconfig.moduleNameMapper,
        "^react-markdown$": "<rootDir>/../../node_modules/react-markdown",
        "^@eccenca/gui-elements$": "<rootDir>/../../node_modules/@eccenca/gui-elements",
        "^@eccenca/gui-elements/(.*)$": "<rootDir>/../../node_modules/@eccenca/gui-elements/$1",
        "^@reduxjs/toolkit$": "<rootDir>/../../node_modules/@reduxjs/toolkit/dist/cjs/redux-toolkit.development.cjs",
    },
};
