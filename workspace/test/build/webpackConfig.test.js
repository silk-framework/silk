process.env.BABEL_ENV = "development";
process.env.NODE_ENV = "development";

const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const webpack = require("webpack");
const configFactory = require("../../config/webpack.di.config");
const paths = require("../../config/paths");

const pluginByName = (config, constructorName) =>
    config.plugins.find((plugin) => plugin.constructor.name === constructorName);

describe("Workspace webpack configuration invariants", () => {
    let oneShotConfig;
    let watchConfig;
    let productionConfig;

    beforeAll(() => {
        oneShotConfig = configFactory("development", false);
        watchConfig = configFactory("development", true);
        productionConfig = configFactory("production", false);
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
        expect(cssRule.use[0]).toBe(MiniCssExtractPlugin.loader);
    });

    it("includes DI sources in forked TypeScript checking", () => {
        const typeChecker = pluginByName(oneShotConfig, "ForkTsCheckerWebpackPlugin");

        expect(typeChecker.options.typescript.configOverwrite.include).toEqual([
            paths.appSrc,
            ...paths.additionalSourcePaths(),
        ]);
    });

    it("constructs DI entries and transpiled source roots directly", () => {
        const loaderRules = oneShotConfig.module.rules.find((rule) => rule.oneOf).oneOf;
        const babelRule = loaderRules.find((rule) => String(rule.test) === String(/\.(js|mjs|jsx|ts|tsx)$/));

        expect(oneShotConfig.entry).toEqual([paths.appIndexJs, ...paths.additionalEntries()]);
        expect(babelRule.include).toEqual([paths.appSrc, paths.guiElements, ...paths.additionalSourcePaths()]);
    });

    it("uses webpack 5 output isolation and native asset modules", () => {
        const loaderRules = oneShotConfig.module.rules.find((rule) => rule.oneOf).oneOf;

        expect(oneShotConfig.output.uniqueName).toBe("workspace");
        expect(oneShotConfig.output).not.toHaveProperty("jsonpFunction");
        expect(oneShotConfig.output).not.toHaveProperty("futureEmitAssets");
        expect(oneShotConfig).not.toHaveProperty("node");
        expect(loaderRules).toEqual(
            expect.arrayContaining([
                expect.objectContaining({ type: "asset" }),
                expect.objectContaining({ type: "asset/resource" }),
            ]),
        );
    });

    it("emits fonts beside extracted CSS and exposes CSS-relative font URLs", () => {
        const loaderRules = oneShotConfig.module.rules.find((rule) => rule.oneOf).oneOf;
        const fontRule = loaderRules.find((rule) => String(rule.test).includes("woff"));

        expect(fontRule.generator).toEqual({
            filename: "[name][ext]",
            outputPath: "assets/css/fonts/",
            publicPath: "fonts/",
        });
    });

    it("uses webpack defaults for JavaScript minimization and a webpack 5 CSS minimizer", () => {
        expect(productionConfig.optimization.minimizer[0]).toBe("...");
        expect(productionConfig.optimization.minimizer[1].constructor.name).toBe("CssMinimizerPlugin");
    });

    it("enables analysis and native profiling only when requested", () => {
        const normalPluginNames = productionConfig.plugins.map((plugin) => plugin.constructor.name);
        const diagnosticConfig = configFactory("production", false, { analyze: true, profile: true });
        const diagnosticPluginNames = diagnosticConfig.plugins.map((plugin) => plugin.constructor.name);

        expect(normalPluginNames).not.toContain("BundleAnalyzerPlugin");
        expect(normalPluginNames).not.toContain("ProfilingPlugin");
        expect(diagnosticConfig.profile).toBe(true);
        expect(diagnosticPluginNames).toEqual(
            expect.arrayContaining(["BundleAnalyzerPlugin", "ProgressPlugin", "ProfilingPlugin"]),
        );
    });

    it("passes webpack 5 configuration validation", () => {
        expect(() => webpack.validate(oneShotConfig)).not.toThrow();
        expect(() => webpack.validate(watchConfig)).not.toThrow();
        expect(() => webpack.validate(productionConfig)).not.toThrow();
    });
});
