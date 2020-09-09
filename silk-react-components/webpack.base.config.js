var webpack = require('webpack');
const path = require('path');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

const fs = require("fs");
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

module.exports = {
  entry: path.resolve(__dirname, 'src','index.jsx'),
  output: {
    path: path.resolve(__dirname, 'public'),
    filename: 'main.js',
  },
  module: {
    rules: [
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
        test: /.(js|jsx)$/,
        include: [
          path.resolve(__dirname, 'src'),
        ],
        exclude: /(node_modules|bower_components)/,
        use: {
          loader: 'babel-loader',
        }
      },
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
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
      {
          test: /\.(svg|png|jpe?g|gif|ico)(\?.+)?$/,
          loader: 'url-loader',
          options: {
              limit: 10000,
              name: 'image/[name].[ext]',
          },
      }
    ]
  },
  resolve: {
    extensions: ['*', '.js', '.jsx', '.ts', '.tsx', '.json'],
    mainFields: ['es5', 'browser', 'module', 'main'],
    alias: {
      "@gui-elements": resolveApp('src/libs/gui-elements'),
    }
  },
  output: {
    path: __dirname + '/dist',
    publicPath: '/',
    filename: 'main.js'
  },
  devtool: "source-map",
  plugins: [
      new webpack.DefinePlugin({ __DEBUG__: false }),
      new MiniCssExtractPlugin({
          // Options similar to the same options in webpackOptions.output
          // both options are optional
          filename: "style.css",
          chunkFilename: "[id].css"
      }),
      function()
      {
          // Outputs compilation errors to stderr
          this.plugin("done", function(stats)
          {
              if (stats.compilation.errors && stats.compilation.errors.length)
              {
                  console.error(stats.compilation.errors);
                  process.exit(1);
              }
          });
      }
  ]
};
