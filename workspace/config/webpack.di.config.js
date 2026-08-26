"use strict";

const fs = require("fs");
const path = require("path");
const webpack = require("webpack");
const sass = require("sass");
const sassRenderSyncOptions = require("@eccenca/gui-elements/config/sassOptions");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const CaseSensitivePathsPlugin = require("case-sensitive-paths-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const InterpolateHtmlPlugin = require("react-dev-utils/InterpolateHtmlPlugin");
const ModuleScopePlugin = require("react-dev-utils/ModuleScopePlugin");
const getCSSModuleLocalIdent = require("react-dev-utils/getCSSModuleLocalIdent");
const ModuleNotFoundPlugin = require("react-dev-utils/ModuleNotFoundPlugin");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");
const { BundleAnalyzerPlugin } = require("webpack-bundle-analyzer");
const { CycloneDxWebpackPlugin } = require("@cyclonedx/webpack-plugin");
const paths = require("./paths");
const getClientEnvironment = require("./env");

const cssRegex = /\.css$/;
const cssModuleRegex = /\.module\.css$/;
const sassRegex = /\.(scss|sass)$/;
const sassModuleRegex = /\.module\.(scss|sass)$/;
const imageRegex = [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/, /\.svg$/];
const fontRegex = /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/;
const useTypeScript = fs.existsSync(paths.appTsConfig);
const appPackageJson = require(paths.appPackageJson);

module.exports = function createWebpackConfig(webpackEnv, isWatch = false, diagnostics = {}) {
    const isEnvDevelopment = webpackEnv === "development";
    const isEnvProduction = webpackEnv === "production";
    const sourcePaths = [paths.appSrc, paths.guiElements, ...paths.additionalSourcePaths()];
    const publicUrl = paths.appDIAssetsUrl;
    const env = getClientEnvironment(publicUrl);
    const buildPath = isWatch ? paths.watchDIBuild : paths.appDIBuild;
    const diagnosticsPath = path.join(paths.appPath, ".local", "webpack");
    const profilingEnabled = diagnostics.profile === true;
    const analysisEnabled = diagnostics.analyze === true;

    if (profilingEnabled || analysisEnabled) {
        fs.mkdirSync(diagnosticsPath, { recursive: true });
    }

    const getStyleLoaders = (cssOptions, preProcessor) => {
        const loaders = [
            MiniCssExtractPlugin.loader,
            {
                loader: require.resolve("css-loader"),
                options: cssOptions,
            },
        ];
        if (preProcessor) {
            loaders.push({
                loader: require.resolve(preProcessor),
                options: {
                    api: "modern",
                    implementation: sass,
                    sassOptions: {
                        ...sassRenderSyncOptions,
                        silenceDeprecations: ["import"],
                    },
                    sourceMap: false,
                },
            });
        }
        return loaders;
    };

    return {
        mode: isEnvProduction ? "production" : "development",
        target: "browserslist",
        bail: isEnvProduction,
        profile: profilingEnabled,
        devtool: isEnvProduction ? "source-map" : "cheap-module-source-map",
        entry: [paths.appIndexJs, ...paths.additionalEntries()],
        output: {
            path: buildPath,
            pathinfo: isEnvDevelopment,
            filename: isEnvProduction ? "assets/js/[name].[contenthash:8].js" : "assets/js/[name].js",
            chunkFilename: isEnvProduction ? "assets/js/[name].[contenthash:8].chunk.js" : "assets/js/[name].chunk.js",
            publicPath: publicUrl,
            uniqueName: appPackageJson.name,
            devtoolModuleFilenameTemplate: isEnvProduction
                ? (info) => path.relative(paths.appSrc, info.absoluteResourcePath).replace(/\\/g, "/")
                : (info) => path.resolve(info.absoluteResourcePath).replace(/\\/g, "/"),
        },
        optimization: {
            minimize: isEnvProduction,
            minimizer: [
                "...",
                new CssMinimizerPlugin({
                    minimizerOptions: {
                        preset: ["default", { minifyFontValues: { removeQuotes: false } }],
                    },
                }),
            ],
            splitChunks: {
                chunks: "all",
            },
            runtimeChunk: {
                name: (entrypoint) => `runtime-${entrypoint.name}`,
            },
        },
        resolve: {
            modules: ["node_modules"].concat(process.env.NODE_PATH.split(path.delimiter).filter(Boolean)),
            extensions: paths.moduleFileExtensions
                .map((extension) => `.${extension}`)
                .filter((extension) => useTypeScript || !extension.includes("ts")),
            alias: {
                "react-native": "react-native-web",
                "@ducks": paths.ducksFolder,
                "@eccenca/gui-elements$": path.join(paths.guiElements, "index.ts"),
                "@eccenca/gui-elements": paths.guiElements,
            },
            plugins: [
                new ModuleScopePlugin(sourcePaths, [
                    paths.appPackageJson,
                    require.resolve("@babel/runtime/package.json"),
                ]),
            ],
        },
        module: {
            strictExportPresence: true,
            rules: [
                {
                    oneOf: [
                        {
                            test: imageRegex,
                            type: "asset",
                            parser: {
                                dataUrlCondition: {
                                    maxSize: 10000,
                                },
                            },
                            generator: {
                                filename: "assets/media/[name].[contenthash:8][ext]",
                            },
                        },
                        {
                            test: /\.(js|mjs|jsx|ts|tsx)$/,
                            include: sourcePaths,
                            loader: require.resolve("babel-loader"),
                            options: {
                                customize: require.resolve("babel-preset-react-app/webpack-overrides"),
                                presets: [["react-app", { flow: false, typescript: true }]],
                                cacheDirectory: true,
                                cacheCompression: false,
                                compact: isEnvProduction,
                            },
                        },
                        {
                            test: /\.(js|mjs)$/,
                            exclude: /@babel(?:\/|\\{1,2})runtime/,
                            loader: require.resolve("babel-loader"),
                            type: "javascript/auto",
                            options: {
                                babelrc: false,
                                configFile: false,
                                compact: false,
                                presets: [[require.resolve("babel-preset-react-app/dependencies"), { helpers: true }]],
                                cacheDirectory: true,
                                cacheCompression: false,
                                sourceMaps: false,
                            },
                        },
                        {
                            test: cssRegex,
                            exclude: cssModuleRegex,
                            use: getStyleLoaders({ importLoaders: 0, sourceMap: false }),
                            sideEffects: true,
                        },
                        {
                            test: cssModuleRegex,
                            use: getStyleLoaders({
                                importLoaders: 0,
                                sourceMap: false,
                                modules: {
                                    namedExport: false,
                                    getLocalIdent: getCSSModuleLocalIdent,
                                },
                            }),
                        },
                        {
                            test: sassRegex,
                            exclude: sassModuleRegex,
                            use: getStyleLoaders({ importLoaders: 1, sourceMap: false }, "sass-loader"),
                            sideEffects: true,
                        },
                        {
                            test: sassModuleRegex,
                            use: getStyleLoaders(
                                {
                                    importLoaders: 1,
                                    sourceMap: false,
                                    modules: {
                                        namedExport: false,
                                        getLocalIdent: getCSSModuleLocalIdent,
                                    },
                                },
                                "sass-loader",
                            ),
                        },
                        {
                            test: fontRegex,
                            type: "asset/resource",
                            generator: {
                                filename: "assets/css/fonts/[name][ext]",
                                publicPath: "fonts/",
                            },
                        },
                        {
                            exclude: [/\.(js|mjs|jsx|ts|tsx)$/, /\.html$/, /\.json$/],
                            type: "asset/resource",
                            generator: {
                                filename: "assets/media/[name].[contenthash:8][ext]",
                            },
                        },
                    ],
                },
            ],
        },
        plugins: [
            new HtmlWebpackPlugin({
                inject: true,
                template: paths.appHtml,
                ...(isEnvProduction
                    ? {
                          minify: {
                              removeComments: true,
                              collapseWhitespace: true,
                              removeRedundantAttributes: true,
                              useShortDoctype: true,
                              removeEmptyAttributes: true,
                              removeStyleLinkTypeAttributes: true,
                              keepClosingSlash: true,
                              minifyJS: true,
                              minifyCSS: true,
                              minifyURLs: true,
                          },
                      }
                    : {}),
            }),
            new InterpolateHtmlPlugin(HtmlWebpackPlugin, env.raw),
            new ModuleNotFoundPlugin(paths.appPath),
            new webpack.DefinePlugin(env.stringified),
            isEnvDevelopment && new CaseSensitivePathsPlugin(),
            new MiniCssExtractPlugin({
                filename: "assets/css/[name].[contenthash:8].css",
                chunkFilename: "assets/css/[name].[contenthash:8].chunk.css",
            }),
            useTypeScript &&
                new ForkTsCheckerWebpackPlugin({
                    typescript: {
                        configOverwrite: {
                            include: [paths.appSrc, ...paths.additionalSourcePaths()],
                        },
                    },
                }),
            profilingEnabled && new webpack.ProgressPlugin({ profile: true }),
            profilingEnabled &&
                new webpack.debug.ProfilingPlugin({
                    outputPath: path.join(diagnosticsPath, "profile-events.json"),
                }),
            analysisEnabled &&
                new BundleAnalyzerPlugin({
                    analyzerMode: "static",
                    openAnalyzer: false,
                    reportFilename: path.join(diagnosticsPath, "bundle-report.html"),
                    generateStatsFile: true,
                    statsFilename: path.join(diagnosticsPath, "bundle-stats.json"),
                }),
            isEnvProduction &&
                new CycloneDxWebpackPlugin({
                    outputLocation: "./artifacts",
                    includeWellknown: false,
                }),
        ].filter(Boolean),
        performance: false,
    };
};
