const path = require("path");
const fs = require("fs");
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

const merge = require('webpack-merge');
const baseConfig = require('./webpack.base.config.js');

baseConfig.output.path = resolveApp('../silk-workbench/silk-workbench-core/public/libs/silk-react-components')

module.exports = merge(baseConfig, {
  mode: 'development',
});
