process.env.BABEL_ENV = "development";
process.env.NODE_ENV = "development";

const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const configFactory = require("../../config/webpack.di.config");
const paths = require("../../config/paths");

const pluginByName = (config, constructorName) =>
    config.plugins.find((plugin) => plugin.constructor.name === constructorName);

describe("Workspace webpack configuration invariants", () => {
    let oneShotConfig;
    let watchConfig;

    beforeAll(() => {
        oneShotConfig = configFactory("development", false);
        watchConfig = configFactory("development", true);
    });

    it("uses the dedicated build path for one-shot builds and the shared Play target for watch", () => {
        expect(oneShotConfig.output.path).toBe(paths.appDIBuild);
        expect(watchConfig.output.path).toBe(paths.watchDIBuild);
    });

    it("keeps the backend asset URL as webpack's public path", () => {
        expect(oneShotConfig.output.publicPath).toBe(paths.appDIAssetsUrl);
    });

    it("extracts CSS in development so Play can serve files written to disk", () => {
        const cssExtractionPlugin = oneShotConfig.plugins.find((plugin) => plugin instanceof MiniCssExtractPlugin);
        const loaderRules = oneShotConfig.module.rules.find((rule) => rule.oneOf).oneOf;
        const cssRule = loaderRules.find((rule) => String(rule.test) === String(/\.css$/));

        expect(cssExtractionPlugin).toBeDefined();
        expect(cssRule.use[0].loader).toBe(MiniCssExtractPlugin.loader);
    });

    it("includes DI sources in forked TypeScript checking", () => {
        const typeChecker = pluginByName(oneShotConfig, "ForkTsCheckerWebpackPlugin");

        expect(typeChecker.options.typescript.configOverwrite.include).toEqual([
            paths.appSrc,
            ...paths.additionalSourcePaths(),
        ]);
    });
});
