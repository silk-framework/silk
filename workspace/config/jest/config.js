/* import type {Config} from "jest"; */

module.exports = {
    rootDir: "./../../",
    testEnvironment: "jest-fixed-jsdom",
    testEnvironmentOptions: {
        url: "http://localhost/",
        globalsCleanup: "on",
    },
    globals: {
        __DEBUG__: false,
    },
    roots: ["<rootDir>/src/", "<rootDir>/test/"],
    collectCoverageFrom: ["test/**/*.{js,jsx,ts,tsx}", "src/**/*.{js,jsx,ts,tsx}", "!src/**/*.d.ts"],
    resolver: "jest-pnp-resolver",
    setupFiles: ["react-app-polyfill/jsdom"],
    setupFilesAfterEnv: ["<rootDir>/src/setupTests.ts"],
    testMatch: [
        "<rootDir>/src/**/*(*.)@(spec|test).{js,jsx,ts,tsx}",
        "<rootDir>/test/**/*(*.)@(spec|test).{js,jsx,ts,tsx}",
    ],
    transform: {
        "^.+\\.(js|jsx|ts|tsx|mjs|cjs)$": "babel-jest",
        "^.+\\.css$": "<rootDir>/config/jest/cssTransform.js",
        "^(?!.*\\.(js|jsx|ts|tsx|css|json)$)": "<rootDir>/config/jest/fileTransform.js",
    },
    transformIgnorePatterns: [
        "[/\\\\]node_modules[/\\\\](?!react-markdown|vfile|unist-util-stringify-position|@reduxjs/toolkit|).+\\.(js|jsx|ts|tsx|mjs|cjs)$",
        "^.+\\.module\\.(css|sass|scss)$",
    ],
    moduleNameMapper: {
        "^react-native$": "react-native-web",
        "^react-markdown$": "<rootDir>/../node_modules/react-markdown",
        "^@eccenca/gui-elements$": "<rootDir>/../node_modules/@eccenca/gui-elements",
        "^@eccenca/gui-elements/(.*)$": "<rootDir>/../node_modules/@eccenca/gui-elements/$1",
        "^@reduxjs/toolkit$": "<rootDir>/../../node_modules/@reduxjs/toolkit/dist/cjs/redux-toolkit.development.cjs",
        "^.+\\.module\\.(css|sass|scss)$": "identity-obj-proxy",
        "@ducks(.*)$": "<rootDir>/src/app/store/ducks/$1",
    },
    moduleFileExtensions: ["web.js", "js", "web.ts", "ts", "web.tsx", "tsx", "json", "web.jsx", "jsx", "node"],
    watchPlugins: ["jest-watch-typeahead/filename", "jest-watch-typeahead/testname"],
};
