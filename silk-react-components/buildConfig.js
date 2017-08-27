const path = require('path');

module.exports = {
    webpackConfig: {
        common: {
            context: path.resolve(__dirname),
            entry: {
                main: './src/index.jsx',
            },
            output: {
                publicPath: '',
                filename: '[name].js',
            },
            html: {
                template: path.resolve(__dirname, 'index.html.ejs'),
                inject: false,
                chunksSortMode: 'id',
            },
            browsers: [
                "Chrome >= 45",
                "Firefox >= 40",
                "edge >= 12",
                "ie 11",
            ],
        },
        debug: {},
        application: {
            externals:{
                '@eccenca/material-design-lite': 'window.componentHandler',
            },
            minify: false,
            output: {
                path: path.resolve(__dirname, 'dist'),
                filename: '[name].js?[chunkhash:5]',
            },
            html: {
                addConfigJS: true,
            },
        },
    },
    lintingFiles: [
        './src/**/*',
        './test/**/*',
    ],
};
