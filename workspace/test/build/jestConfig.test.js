const { transformIgnorePatterns } = require("../../config/jest/config");

const ignoresTransform = (file) => transformIgnorePatterns.some((pattern) => new RegExp(pattern).test(file));

describe("Jest dependency transforms", () => {
    it.each([
        "/repo/node_modules/react/index.js",
        "/repo/node_modules/lodash/lodash.js",
        "/repo/node_modules/@reduxjs/toolkit/dist/cjs/redux-toolkit.development.cjs",
        "/repo/node_modules/react-markdown-extra/index.js",
        "/repo/node_modules/@uppy-extra/core/index.js",
        "/repo/node_modules/react-markdown/node_modules/lodash/index.js",
        "C:\\repo\\node_modules\\react\\index.js",
    ])("does not transform CommonJS dependencies: %s", (file) => {
        expect(ignoresTransform(file)).toBe(true);
    });

    it.each([
        "/repo/src/App.tsx",
        "/repo/libs/gui-elements/index.ts",
        "/repo/node_modules/react-markdown/index.js",
        "/repo/node_modules/@uppy/core/lib/index.js",
        "/repo/node_modules/color/index.js",
        "/repo/node_modules/d3-selection/src/index.js",
        "/repo/node_modules/hast-util-to-jsx-runtime/lib/index.js",
        "/repo/node_modules/parent/node_modules/@uppy/core/lib/index.js",
        "C:\\repo\\node_modules\\react-markdown\\index.js",
        "C:\\repo\\node_modules\\parent\\node_modules\\@uppy\\core\\lib\\index.js",
    ])("transforms application sources and supported ESM dependencies: %s", (file) => {
        expect(ignoresTransform(file)).toBe(false);
    });

    it("keeps CSS modules out of Babel", () => {
        expect(ignoresTransform("/repo/src/App.module.scss")).toBe(true);
    });
});
