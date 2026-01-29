import type {Config} from "jest";
import orgconfig from "./config.ts"

const subconfig: Config = {
    ...orgconfig,
    "moduleNameMapper": {
        ...orgconfig.moduleNameMapper,
        "^react-markdown$": "<rootDir>/../../node_modules/react-markdown",
        "^@eccenca/gui-elements$": "<rootDir>/../../node_modules/@eccenca/gui-elements",
        "^@eccenca/gui-elements/(.*)$": "<rootDir>/../../node_modules/@eccenca/gui-elements/$1",
    },
};

export default subconfig;
