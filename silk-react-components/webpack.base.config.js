const path = require("path");
const webpack = require("webpack");
const resolve = require("resolve");
const PnpWebpackPlugin = require("pnp-webpack-plugin");
const TerserPlugin = require("terser-webpack-plugin");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const WorkboxWebpackPlugin = require("workbox-webpack-plugin");
const WatchMissingNodeModulesPlugin = require("react-dev-utils/WatchMissingNodeModulesPlugin");
const ModuleNotFoundPlugin = require("react-dev-utils/ModuleNotFoundPlugin");
const ForkTsCheckerWebpackPlugin = require("react-dev-utils/ForkTsCheckerWebpackPlugin");
const typescriptFormatter = require("react-dev-utils/typescriptFormatter");
const getCSSModuleLocalIdent = require("react-dev-utils/getCSSModuleLocalIdent");
const OptimizeCSSAssetsPlugin = require('optimize-css-assets-webpack-plugin');
const ModuleScopePlugin = require("react-dev-utils/ModuleScopePlugin");

const isEnvDevelopment = process.env.NODE_ENV === "development";
const isEnvProduction = process.env.NODE_ENV === "production";
const postcssNormalize = require("postcss-normalize");

const fs = require("fs");
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

module.exports = {
    entry: resolveApp('src/index.jsx'),
    // Stop compilation early in production
    bail: isEnvProduction,
    devtool: isEnvProduction ? "source-map" : isEnvDevelopment && "cheap-module-source-map",
    output: {
        path: resolveApp('dist'),
        publicPath: '/',
        // There will be one main bundle, and one file per asynchronous chunk.
        // In development, it does not produce real files.
        pathinfo: isEnvDevelopment,
        filename: "main.js",
        // TODO: remove this when upgrading to webpack 5
        futureEmitAssets: true,
        // this defaults to 'window', but by setting it to 'this' then
        // module chunks which are built will work in web workers as well.
        globalObject: "this",
    },
    optimization: {
        minimize: isEnvProduction,
        minimizer: [
            // This is only used in production mode
            new TerserPlugin({
                terserOptions: {
                    parse: {
                        // we want terser to parse ecma 8 code. However, we don't want it
                        // to apply any minfication steps that turns valid ecma 5 code
                        // into invalid ecma 5 code. This is why the 'compress' and 'output'
                        // sections only apply transformations that are ecma 5 safe
                        // https://github.com/facebook/create-react-app/pull/4234
                        ecma: 8,
                    },
                    compress: {
                        ecma: 5,
                        warnings: false,
                        // Disabled because of an issue with Uglify breaking seemingly valid code:
                        // https://github.com/facebook/create-react-app/issues/2376
                        // Pending further investigation:
                        // https://github.com/mishoo/UglifyJS2/issues/2011
                        comparisons: false,
                        // Disabled because of an issue with Terser breaking valid code:
                        // https://github.com/facebook/create-react-app/issues/5250
                        // Pending futher investigation:
                        // https://github.com/terser-js/terser/issues/120
                        inline: 2,
                    },
                    mangle: {
                        safari10: true,
                    },
                    output: {
                        ecma: 5,
                        comments: false,
                        // Turned on because emoji and regex is not minified properly using default
                        // https://github.com/facebook/create-react-app/issues/2488
                        ascii_only: true,
                    },
                },
                sourceMap: true,
            }),
            new OptimizeCSSAssetsPlugin({})
        ],
        // splitChunks: {
        //     cacheGroups: {
        //         styles: {
        //             name: 'style',
        //             test: /\.css$/,
        //             chunks: 'all',
        //             enforce: true,
        //         },
        //     },
        // },
    },
    resolve: {
        // This allows you to set a fallback for where webpack should look for modules.
        // We placed these paths second because we want `node_modules` to "win"
        // if there are any conflicts. This matches Node resolution mechanism.
        // https://github.com/facebook/create-react-app/issues/253
        modules: ["node_modules"],
        // These are the reasonable defaults supported by the Node ecosystem.
        // We also include JSX as a common component filename extension to support
        // some tools, although we do not recommend using it, see:
        // https://github.com/facebook/create-react-app/issues/290
        // `web` extension prefixes have been added for better support
        // for React Native Web.
        extensions: [
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
            ".json"
        ].map((ext) => `.${ext}`),
        mainFields: ['es5', 'browser', 'module', 'main'],
        alias: {
            "@gui-elements": resolveApp('src/libs/gui-elements'),
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
            new ModuleScopePlugin(paths.appSrc, [paths.appPackageJson]),
        ],
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
        rules: [
            // Disable require.ensure as it's not a standard language feature.
            {parser: {requireEnsure: false}},
            // "url" loader works like "file" loader except that it embeds assets
            // smaller than specified limit in bytes as data URLs to avoid requests.
            // A missing `test` is equivalent to a match.
            {
                test: [/\.bmp$/, /\.gif$/, /\.jpe?g$/, /\.png$/, /\.svg$/, /\.ico$/],
                loader: require.resolve("url-loader"),
                options: {
                    limit: 10000,
                    name: 'image/[name].[ext]',
                },
            },
            // // Process application JS with Babel.
            // // The preset includes JSX, Flow, TypeScript, and some ESnext features.
            {
                test: /\.(js|mjs|jsx|ts|tsx)$/,
                include: resolveApp('src'),
                exclude: /(node_modules|bower_components)/,
                loader: require.resolve("babel-loader"),
                options: {
                    presets: [
                        ["react-app", {"flow": false, "typescript": true}],
                    ],
                    customize: require.resolve("babel-preset-react-app/webpack-overrides"),
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
            {
                test: /\.(js|mjs)$/,
                exclude: /@babel(?:\/|\\{1,2})runtime/,
                loader: require.resolve("babel-loader"),
                options: {
                    babelrc: false,
                    configFile: false,
                    compact: false,
                    presets: [[require.resolve("babel-preset-react-app/dependencies"), {helpers: true}]],
                    cacheDirectory: true,
                    cacheCompression: false,
                    
                    // If an error happens in a package, it's possible to be
                    // because it was compiled. Thus, we don't want the browser
                    // debugger to show the original code. Instead, the code
                    // being evaluated would be much more helpful.
                    sourceMaps: false,
                },
            },
            {
                test: /\.css$/,
                loader: 'style-loader'
            },
            {
                test: /\.scss$/,
                exclude: /(node_modules|bower_components)/,
                use: [
                    {
                        loader: MiniCssExtractPlugin.loader // creates style nodes from JS strings
                    },
                    {
                        loader: "css-loader" // translates CSS into CommonJS
                    },
                    {
                        loader: "sass-loader" // compiles Sass to CSS
                    }
                ]
            },
            {
                test: /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/,
                use: [{
                    loader: 'file-loader',
                    options: {
                        name: '[name].[ext]',
                        outputPath: 'fonts/',
                        publicPath: 'fonts'
                    }
                }]
            },
        ]
    },
    plugins: [
        // This gives some necessary context to module not found errors, such as
        // the requesting resource.
        new ModuleNotFoundPlugin(resolveApp('.')),
        new webpack.DefinePlugin({__DEBUG__: false}),
        // If you require a missing module and then `npm install` it, you still have
        // to restart the development server for Webpack to discover it. This plugin
        // makes the discovery automatic so you don't have to restart.
        // See https://github.com/facebook/create-react-app/issues/186
        isEnvDevelopment && new WatchMissingNodeModulesPlugin(resolveApp('node_modules')),
        new MiniCssExtractPlugin({
            // Options similar to the same options in webpackOptions.output
            // both options are optional
            filename: "style.css",
            chunkFilename: "[id].css"
        }),
        // Moment.js is an extremely popular library that bundles large locale files
        // by default due to how Webpack interprets its code. This is a practical
        // solution that requires the user to opt into importing specific locales.
        // https://github.com/jmblog/how-to-optimize-momentjs-with-webpack
        // You can remove this if you don't use Moment.js:
        new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/),
        new ForkTsCheckerWebpackPlugin({
            typescript: resolve.sync("typescript", {
                basedir: resolveApp('node_modules'),
            }),
            useTypescriptIncrementalApi: true,
            checkSyntacticErrors: true,
            measureCompilationTime: false,
            tsconfig: resolveApp('tsconfig.json'),
            resolveModuleNameModule: process.versions.pnp ? `${__dirname}/pnpTs.js` : undefined,
            resolveTypeReferenceDirectiveModule: process.versions.pnp ? `${__dirname}/pnpTs.js` : undefined,
            reportFiles: [
                "**",
                "!**/__tests__/**",
                "!**/test/**",
                "!**/?(*.)(spec|test).*",
                "!**/src/setupProxy.*",
                "!**/src/setupTests.*",
            ],
            watch: resolveApp('src'),
            async: isEnvDevelopment,
            silent: isEnvProduction,
            formatter: !isEnvDevelopment ? typescriptFormatter : undefined,
        }),
        // function () {
        //     // Outputs compilation errors to stderr
        //     this.plugin("done", function (stats) {
        //         if (stats.compilation.errors && stats.compilation.errors.length) {
        //             console.error(stats.compilation.errors);
        //             process.exit(1);
        //         }
        //     });
        // },
    ].filter(Boolean),
    // Some libraries import Node modules but don't use them in the browser.
    // Tell Webpack to provide empty mocks for them so importing them works.
    node: {
        module: "empty",
        dgram: "empty",
        dns: "mock",
        fs: "empty",
        net: "empty",
        tls: "empty",
        child_process: "empty",
    },
    // Turn off performance processing because we utilize
    // our own hints via the FileSizeReporter
    performance: false,
};
