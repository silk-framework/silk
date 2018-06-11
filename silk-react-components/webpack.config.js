var webpack = require('webpack');
const path = require('path');

module.exports = {
  mode: 'production',
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
        use: [
          {
            loader: "style-loader" // creates style nodes from JS strings
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
    ]
  },
  resolve: {
    extensions: ['*', '.js', '.jsx']
  },
  output: {
    path: __dirname + '/dist',
    publicPath: '/',
    filename: 'main.js'
  },
  
  plugins: [ new webpack.DefinePlugin({ __DEBUG__: false })]
};