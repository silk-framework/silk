"use strict";

const fs = require("fs");
const path = require("path");
const webpack = require("webpack");
const resolve = require("resolve");
const sass = require("sass");
const sassRenderSyncOptions = require("@eccenca/gui-elements/config/sassOptions");
const PnpWebpackPlugin = require("pnp-webpack-plugin");
const HtmlWebpackPlugin = require("html-webpack-plugin");
const CaseSensitivePathsPlugin = require("case-sensitive-paths-webpack-plugin");
// const InlineChunkHtmlPlugin = require("react-dev-utils/InlineChunkHtmlPlugin");
const TerserPlugin = require("terser-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CssMinimizerPlugin = require("css-minimizer-webpack-plugin");
const { WebpackManifestPlugin } = require("webpack-manifest-plugin");
const InterpolateHtmlPlugin = require("react-dev-utils/InterpolateHtmlPlugin");
const WorkboxWebpackPlugin = require("workbox-webpack-plugin");
const ModuleScopePlugin = require("react-dev-utils/ModuleScopePlugin");
const getCSSModuleLocalIdent = require("react-dev-utils/getCSSModuleLocalIdent");
const paths = require("./paths");
const getClientEnvironment = require("./env");
const ModuleNotFoundPlugin = require("react-dev-utils/ModuleNotFoundPlugin");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin"); //https://github.com/TypeStrong/fork-ts-checker-webpack-plugin/issues/797
const BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;
const { CycloneDxWebpackPlugin } = require("@cyclonedx/webpack-plugin");

// Some apps do not need the benefits of saving a web request, so not inlining the chunk
// makes for a smoother build process.
const shouldInlineRuntimeChunk = process.env.INLINE_RUNTIME_CHUNK !== "false";
// Check if TypeScript is setup
const useTypeScript = fs.existsSync(paths.appTsConfig);

// style files regexes
const cssRegex = /\.css$/;
const cssModuleRegex = /\.module\.css$/;
const sassRegex = /\.(scss|sass)$/;
const sassModuleRegex = /\.module\.(scss|sass)$/;
const appPackageJson = require(paths.appPackageJson);

// This is the production and development configuration.
// It is focused on developer experience, fast rebuilds, and a minimal bundle.
module.exports = function (webpackEnv, isWatch) {
    const isEnvDevelopment = webpackEnv === "development";
    const isEnvProduction = webpackEnv === "production";

    // Webpack uses `publicPath` to determine where the app is being served from.
    // It requires a trailing slash, or the file assets will get an incorrect path.
    // In development, we always serve from the root. This makes config easier.
    const publicPath = "/";

    // Some apps do not use client-side routing with pushState.
    // For these, "homepage" can be set to "." to enable relative asset paths.
    const shouldUseRelativeAssetPaths = publicPath === "./";

    // `publicUrl` is just like `publicPath`, but we will provide it to our app
    // as %PUBLIC_URL% in `index.html` and `process.env.PUBLIC_URL` in JavaScript.
    // Omit trailing slash as %PUBLIC_URL%/xyz looks better than %PUBLIC_URL%xyz.
    const publicUrl = paths.appDIAssetsUrl;

    // Get environment variables to inject into our app.
    const env = getClientEnvironment(publicUrl);

    // Set the another build path for webpack watch
    const buildPath = isWatch ? paths.watchDIBuild : paths.appDIBuild;

    // common function to get style loaders
    const getStyleLoaders = (cssOptions, preProcessor) => {
        const loaders = [
            // Extract CSS into separate files
            {
                loader: MiniCssExtractPlugin.loader,
                options: {
                    publicPath: "../../", // adjust paths for CSS
                },
            },
            // Translates CSS into CommonJS
            {
                loader: require.resolve("css-loader"),
                options: {
                    ...cssOptions,
                    sourceMap: true, // must be true for resolve-url-loader
                },
            },
            // Apply PostCSS transformations like autoprefixer
            {
                loader: require.resolve("postcss-loader"),
                options: {
                    sourceMap: true, // must be true for resolve-url-loader
                    postcssOptions: {
                        plugins: [require("autoprefixer")()],
                    },
                },
            },
        ];

        // Add resolve-url-loader if a preProcessor is specified (like sass-loader)
        if (preProcessor) {
            loaders.push(
                {
                    loader: require.resolve("resolve-url-loader"),
                    options: {
                        sourceMap: true,
                    },
                },
                {
                    loader: require.resolve(preProcessor),
                    options: {
                        sourceMap: true, // critical for resolve-url-loader
                        implementation: sass,
                        sassOptions: sassRenderSyncOptions,
                    },
                },
            );
        }

        return loaders.filter(Boolean);
    };

    const babelRuntimePath = path.dirname(require.resolve("@babel/runtime/package.json"));

    const config = {
        mode: isEnvProduction ? "production" : isEnvDevelopment && "development",
        // Stop compilation early in production
        bail: isEnvProduction,
        devtool: isEnvProduction ? "source-map" : isEnvDevelopment && "cheap-module-source-map",
        // These are the "entry points" to our application.
        // This means they will be the "root" imports that are included in JS bundle.
        entry: [
            // Include an alternative client for WebpackDevServer. A client's job is to
            // connect to WebpackDevServer by a socket and get notified about changes.
            // When you save a file, the client will either apply hot updates (in case
            // of CSS changes), or refresh the page (in case of JS changes). When you
            // make a syntax error, this client will display a syntax error overlay.
            // Note: instead of the default WebpackDevServer client, we use a custom one
            // to bring better experience for Create React App users. You can replace
            // the line below with these two lines if you prefer the stock client:
            // require.resolve('webpack-dev-server/client') + '?/',
            // require.resolve('webpack/hot/dev-server'),
            // isEnvDevelopment &&
            //   require.resolve('react-dev-utils/webpackHotDevClient'),
            // Finally, this is your app's code:
            paths.appIndexJs,
            // We include the app code last so that if there is a runtime error during
            // initialization, it doesn't blow up the WebpackDevServer client, and
            // changing JS code would still trigger a refresh.
        ].filter(Boolean),
        output: {
            // The build folder.
            path: buildPath,
            // Add /* filename */ comments to generated require()s in the output.
            pathinfo: isEnvDevelopment,
            // There will be one main bundle, and one file per asynchronous chunk.
            // In development, it does not produce real files.
            filename: isEnvProduction
                ? "assets/js/[name].[contenthash:8].js"
                : isEnvDevelopment && "assets/js/[name].js",
            // There are also additional JS chunk files if you use code splitting.
            chunkFilename: isEnvProduction
                ? "assets/js/[name].[contenthash:8].chunk.js"
                : isEnvDevelopment && "assets/js/[name].chunk.js",
            // We inferred the "public path" (such as / or /my-project) from homepage.
            // We use "/" in development
            publicPath: publicUrl,
            // Point sourcemap entries to original disk location (format as URL on Windows)
            devtoolModuleFilenameTemplate: isEnvProduction
                ? (info) => path.relative(paths.appSrc, info.absoluteResourcePath).replace(/\\/g, "/")
                : isEnvDevelopment && ((info) => path.resolve(info.absoluteResourcePath).replace(/\\/g, "/")),
            // Prevents conflicts when multiple webpack runtimes (from different apps)
            // are used on the same page.
            // chunkLoadingGlobal: `webpackChunk${appPackageJson.name}`,
            // this defaults to 'window', but by setting it to 'this' then
            // module chunks which are built will work in web workers as well.
            globalObject: "window",
        },
        optimization: {
            minimize: isEnvProduction,
            minimizer: [
                "...", // IMPORTANT: keeps default minimizers

                new TerserPlugin({
                    parallel: true,
                    terserOptions: {
                        ie8: false,
                        parse: {
                            ecma: 8,
                        },
                        compress: {
                            ecma: 5,
                            comparisons: false,
                            inline: true,
                        },
                        mangle: {
                            safari10: true,
                        },
                        format: {
                            comments: false,
                        },
                    },
                }),

                new CssMinimizerPlugin({
                    minimizerOptions: {
                        preset: [
                            "default",
                            {
                                minifyFontValues: {
                                    removeQuotes: false,
                                },
                            },
                        ],
                    },
                }),
            ],

            splitChunks: {
                chunks: "all",
                name: false,
            },

            runtimeChunk: {
                name: (entrypoint) => `runtime-${entrypoint.name}`,
            },
        },
        resolve: {
            // This allows you to set a fallback for where webpack should look for modules.
            // We placed these paths second because we want `node_modules` to "win"
            // if there are any conflicts. This matches Node resolution mechanism.
            // https://github.com/facebook/create-react-app/issues/253
            modules: ["node_modules"].concat(
                // It is guaranteed to exist because we tweak it in `env.js`
                process.env.NODE_PATH.split(path.delimiter).filter(Boolean),
            ),
            // These are the reasonable defaults supported by the Node ecosystem.
            // We also include JSX as a common component filename extension to support
            // some tools, although we do not recommend using it, see:
            // https://github.com/facebook/create-react-app/issues/290
            // `web` extension prefixes have been added for better support
            // for React Native Web.
            extensions: paths.moduleFileExtensions
                .map((ext) => `.${ext}`)
                .filter((ext) => useTypeScript || !ext.includes("ts")),
            alias: {
                // Support React Native Web
                // https://www.smashingmagazine.com/2016/08/a-glimpse-into-the-future-with-react-native-for-web/
                "react-native": "react-native-web",
                "@ducks": paths.ducksFolder,
                "@eccenca/gui-elements": paths.guiElements,

                "babel-runtime/core-js/object/assign": "@babel/runtime-corejs3/core-js/object/assign",
                "babel-runtime/core-js/object/keys": "@babel/runtime-corejs3/core-js/object/keys",
                "babel-runtime": "@babel/runtime",
                "babel-runtime/core-js": "@babel/runtime-corejs3/core-js",
                "babel-runtime/helpers": "@babel/runtime/helpers",
                "babel-runtime/regenerator": "@babel/runtime/regenerator",
                // FIXME: webpack4 does not use the `exports` field from `package.json`
                // this was added in webpack5, see https://github.com/webpack/webpack/issues/9509
                // a few packages use only `exports` (and not `modue` or `main`)
                // we replace it here as polyfill until we upgrade the bundler
                // devlop: "devlop/lib/default",
                // "unist-util-visit-parents/do-not-use-color": "unist-util-visit-parents/lib/color",
                // "vfile/do-not-use-conditional-minpath": "vfile/lib/minpath.browser",
                // "vfile/do-not-use-conditional-minproc": "vfile/lib/minproc.browser",
                // "vfile/do-not-use-conditional-minurl": "vfile/lib/minurl.browser",
            },
            plugins: [
                // Adds support for installing with Plug'n'Play, leading to faster installs and adding
                // guards against forgotten dependencies and such.
                PnpWebpackPlugin,
                // Prevents users from importing files from outside of src/ (or node_modules/).
                // This often causes confusion because we only process files within src/ with babel.
                // To fix this, we prevent you from importing files out of src/ -- if you'd like to,
                // please link the files into your node_modules/ and let module-resolution kick in.
                // Make sure your source files are compiled, as they will not be processed in any way.
                new ModuleScopePlugin(paths.appSrc, [paths.appPackageJson, paths.guiElements, babelRuntimePath]),
            ],
            //Allow non-fully-specified imports for ESM packages
            fullySpecified: false,
            // Some libraries import Node modules but don't use them in the browser.
            // Tell Webpack to provide empty mocks for them so importing them works.
            fallback: {
                assert: require.resolve("assert/"),
                buffer: require.resolve("buffer/"),
                process: require.resolve("process/browser"),
                // Set everything else to false
                console: false,
                constants: false,
                crypto: false,
                domain: false,
                events: false,
                http: false,
                https: false,
                os: false,
                path: false,
                punycode: false,
                querystring: false,
                stream: false,
                string_decoder: false,
                sys: false,
                timers: false,
                tty: false,
                url: false,
                util: false,
                vm: false,
                zlib: false,
                fs: false,
                net: false,
                tls: false,
                dgram: false,
                child_process: false,
                dns: false,
                module: false,
            },
        },
        resolveLoader: {
            plugins: [
                // Also related to Plug'n'Play, but this time it tells Webpack to load its loaders
                // from the current package.
                PnpWebpackPlugin.moduleLoader(module),
            ],
        },
        module: {
            strictExportPresence: true,
            parser: {
                javascript: {
                    exportsPresence: "error",
                },
            },
            rules: [
                {
                    test: /\.m?js$/,
                    resolve: {
                        fullySpecified: false,
                    },
                },
                // // Disable require.ensure as it's not a standard language feature.
                // { parser: { requireEnsure: false } },
                {
                    // "oneOf" will traverse all following loaders until one will
                    // match the requirements. When no loader matches it will fall
                    // back to the "file" loader at the end of the loader list.
                    oneOf: [
                        // "url" loader works like "file" loader except that it embeds assets
                        // smaller than specified limit in bytes as data URLs to avoid requests.
                        // A missing `test` is equivalent to a match.
                        {
                            test: [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/, /\.svg$/],
                            type: "asset",
                            generator: {
                                filename: "assets/media/[name].[hash:8][ext]",
                            },
                        },
                        // Process application JS with Babel.
                        // The preset includes JSX, Flow, TypeScript, and some ESnext features.
                        {
                            test: /\.(js|mjs|jsx|ts|tsx)$/,
                            include: [paths.appSrc, paths.guiElements],
                            loader: require.resolve("babel-loader"),
                            options: {
                                customize: require.resolve("babel-preset-react-app/webpack-overrides"),
                                presets: [["react-app", { flow: false, typescript: true }]],
                                plugins: [
                                    [
                                        require.resolve("babel-plugin-named-asset-import"),
                                        {
                                            loaderMap: {
                                                svg: {
                                                    ReactComponent: "@svgr/webpack?-svgo,+ref![path]",
                                                },
                                            },
                                        },
                                    ],
                                ],
                                // This is a feature of `babel-loader` for webpack (not Babel itself).
                                // It enables caching results in ./node_modules/.cache/babel-loader/
                                // directory for faster rebuilds.
                                cacheDirectory: true,
                                cacheCompression: false,
                                compact: isEnvProduction,
                            },
                        },
                        // Process any JS outside of the app with Babel.
                        // Unlike the application JS, we only compile the standard ES features.
                        {
                            test: /\.(js|mjs)$/,
                            exclude: /@babel(?:\/|\\{1,2})runtime/,
                            loader: require.resolve("babel-loader"),
                            options: {
                                babelrc: false,
                                configFile: false,
                                compact: false,
                                presets: [[require.resolve("babel-preset-react-app/dependencies"), { helpers: true }]],
                                cacheDirectory: true,
                                cacheCompression: false,

                                // If an error happens in a package, it's possible to be
                                // because it was compiled. Thus, we don't want the browser
                                // debugger to show the original code. Instead, the code
                                // being evaluated would be much more helpful.
                                sourceMaps: false,
                            },
                        },
                        // "postcss" loader applies autoprefixer to our CSS.
                        // "css" loader resolves paths in CSS and adds assets as dependencies.
                        // "style" loader turns CSS into JS modules that inject <style> tags.
                        // In production, we use MiniCSSExtractPlugin to extract that CSS
                        // to a file, but in development "style" loader enables hot editing
                        // of CSS.
                        // By default we support CSS Modules with the extension .module.css
                        {
                            test: cssRegex,
                            exclude: cssModuleRegex,
                            use: getStyleLoaders({
                                importLoaders: 2,
                                sourceMap: false,
                            }),
                            // Don't consider CSS imports dead code even if the
                            // containing package claims to have no side effects.
                            // Remove this when webpack adds a warning or an error for this.
                            // See https://github.com/webpack/webpack/issues/6571
                            sideEffects: true,
                        },
                        // Adds support for CSS Modules (https://github.com/css-modules/css-modules)
                        // using the extension .module.css
                        {
                            test: cssModuleRegex,
                            use: getStyleLoaders({
                                importLoaders: 2,
                                sourceMap: true,
                                modules: {
                                    getLocalIdent: getCSSModuleLocalIdent,
                                },
                            }),
                        },
                        // Opt-in support for SASS (using .scss or .sass extensions).
                        // By default we support SASS Modules with the
                        // extensions .module.scss or .module.sass
                        {
                            test: sassRegex,
                            exclude: sassModuleRegex,
                            use: getStyleLoaders(
                                {
                                    importLoaders: 4,
                                    sourceMap: true,
                                },
                                "sass-loader",
                            ),
                            // Don't consider CSS imports dead code even if the
                            // containing package claims to have no side effects.
                            // Remove this when webpack adds a warning or an error for this.
                            // See https://github.com/webpack/webpack/issues/6571
                            sideEffects: true,
                        },
                        // Adds support for CSS Modules, but using SASS
                        // using the extension .module.scss or .module.sass
                        {
                            test: sassModuleRegex,
                            use: getStyleLoaders(
                                {
                                    importLoaders: 4,
                                    sourceMap: false,
                                    modules: {
                                        getLocalIdent: getCSSModuleLocalIdent,
                                    },
                                },
                                "sass-loader",
                            ),
                        },
                        {
                            test: /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/,
                            type: "asset/resource",
                            generator: {
                                filename: "assets/css/fonts/[name][ext][query]",
                            },
                        },
                        // "file" loader makes sure those assets get served by WebpackDevServer.
                        // When you `import` an asset, you get its (virtual) filename.
                        // In production, they would get copied to the `build` folder.
                        // This loader doesn't use a "test" so it will catch all modules
                        // that fall through the other loaders.
                        {
                            // Exclude `js` files to keep "css" loader working as it injects
                            // its runtime that would otherwise be processed through "file" loader.
                            // Also exclude `html` and `json` extensions so they get processed
                            // by webpacks internal loaders.
                            exclude: [/\.(js|mjs|jsx|ts|tsx)$/, /\.html$/, /\.json$/],
                            type: "asset/resource",
                            generator: {
                                filename: "assets/media/[name].[hash:8][ext]",
                            },
                        },
                        // ** STOP ** Are you adding a new loader?
                        // Make sure to add the new loader(s) before the "file" loader.
                    ],
                },
            ],
        },
        plugins: [
            // Generates an `index.html` file with the <script> injected.
            new HtmlWebpackPlugin(
                Object.assign(
                    {},
                    {
                        inject: true,
                        template: paths.appHtml,
                    },
                    isEnvProduction
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
                        : undefined,
                ),
            ),
            // Inlines the webpack runtime script. This script is too small to warrant
            // a network request.
            // isEnvProduction &&
            //     shouldInlineRuntimeChunk &&
            //     new InlineChunkHtmlPlugin(HtmlWebpackPlugin, [/runtime~.+[.]js/]),
            // Makes some environment variables available in index.html.
            // The public URL is available as %PUBLIC_URL% in index.html, e.g.:
            // <link rel="shortcut icon" href="%PUBLIC_URL%/favicon.ico">
            // In production, it will be an empty string unless you specify "homepage"
            // in `package.json`, in which case it will be the pathname of that URL.
            // In development, this will be an empty string.
            new InterpolateHtmlPlugin(HtmlWebpackPlugin, env.raw),
            // This gives some necessary context to module not found errors, such as
            // the requesting resource.
            new ModuleNotFoundPlugin(paths.appPath),

            new webpack.NormalModuleReplacementPlugin(/^babel-runtime\/core-js\//, (resource) => {
                resource.request = resource.request.replace(
                    /^babel-runtime\/core-js\//,
                    "@babel/runtime-corejs3/core-js/",
                );
            }),
            new webpack.NormalModuleReplacementPlugin(/^babel-runtime\/helpers\//, (resource) => {
                resource.request = resource.request.replace(/^babel-runtime\/helpers\//, "@babel/runtime/helpers/");
            }),
            new webpack.NormalModuleReplacementPlugin(/^babel-runtime\/regenerator$/, (resource) => {
                resource.request = "@babel/runtime/regenerator";
            }),

            new webpack.ProvidePlugin({
                process: "process/browser",
                Buffer: ["buffer", "Buffer"],
            }),

            // Makes some environment variables available to the JS code, for example:
            // if (process.env.NODE_ENV === 'production') { ... }. See `./env.js`.
            // It is absolutely essential that NODE_ENV is set to production
            // during a production build.
            // Otherwise React will be compiled in the very slow development mode.
            new webpack.DefinePlugin(env.stringified),
            // This is necessary to emit hot updates (currently CSS only):
            // isEnvDevelopment && new webpack.HotModuleReplacementPlugin(),
            // Watcher doesn't work well if you mistype casing in a path so we use
            // a plugin that prints an error when you attempt to do this.
            // See https://github.com/facebook/create-react-app/issues/240
            isEnvDevelopment && new CaseSensitivePathsPlugin(),

            new MiniCssExtractPlugin({
                // Options similar to the same options in webpackOptions.output
                // both options are optional
                filename: "assets/css/[name].[contenthash:8].css",
                chunkFilename: "assets/css/[name].[contenthash:8].chunk.css",
            }),
            // Generate an asset manifest file with the following content:
            // - "files" key: Mapping of all asset filenames to their corresponding
            //   output file so that tools can pick it up without having to parse
            //   `index.html`
            // - "entrypoints" key: Array of files which are included in `index.html`,
            //   can be used to reconstruct the HTML if necessary
            new WebpackManifestPlugin({
                fileName: "asset-manifest.json",
                publicPath: publicPath,
                generate: (seed, files, entrypoints) => {
                    const manifestFiles = files.reduce((manifest, file) => {
                        manifest[file.name] = file.path;
                        return manifest;
                    }, seed);
                    const entrypointFiles = entrypoints.main.filter((fileName) => !fileName.endsWith(".map"));
                    return {
                        files: manifestFiles,
                        entrypoints: entrypointFiles,
                    };
                },
            }),
            // Moment.js is an extremely popular library that bundles large locale files
            // by default due to how Webpack interprets its code. This is a practical
            // solution that requires the user to opt into importing specific locales.
            // https://github.com/jmblog/how-to-optimize-momentjs-with-webpack
            // You can remove this if you don't use Moment.js:
            new webpack.IgnorePlugin({
                resourceRegExp: /^\.\/locale$/,
                contextRegExp: /moment$/,
            }),
            // Generate a service worker script that will precache, and keep up to date,
            // the HTML & assets that are part of the Webpack build.
            isEnvProduction &&
                new WorkboxWebpackPlugin.GenerateSW({
                    clientsClaim: true,
                    exclude: [/\.map$/, /asset-manifest\.json$/],
                    importWorkboxFrom: "cdn",
                    navigateFallback: publicUrl + "/index.html",
                    navigateFallbackBlacklist: [
                        // Exclude URLs starting with /_, as they're likely an API call
                        new RegExp("^/_"),
                        // Exclude URLs containing a dot, as they're likely a resource in
                        // public/ and not a SPA route
                        new RegExp("/[^/]+\\.[^/]+$"),
                    ],
                }),
            // TypeScript type checking
            useTypeScript &&
                new ForkTsCheckerWebpackPlugin({
                    typescript: {
                        configOverwrite: {
                            include: [paths.appSrc, ...paths.additionalSourcePaths()],
                        },
                    },
                }),
            // isEnvProduction && new BundleAnalyzerPlugin({
            //     generateStatsFile: true
            // }),
            isEnvProduction &&
                new CycloneDxWebpackPlugin({
                    outputLocation: "./artifacts",
                }),
        ].filter(Boolean),
        // Turn off performance processing because we utilize
        // our own hints via the FileSizeReporter
        performance: false,
    };

    if (isEnvProduction) {
        const SpeedMeasurePlugin = require("speed-measure-webpack-plugin");
        const smp = new SpeedMeasurePlugin({
            outputFormat: "humanVerbose",
        });
        return smp.wrap(config);
    }

    return config;
};
