const path = require("path");
const fs = require("fs");
const appDirectory = fs.realpathSync(process.cwd());
const resolveApp = (relativePath) => path.resolve(appDirectory, relativePath);

const merge = require('webpack-merge');
const baseConfig = require('./webpack.base.config.js');

baseConfig.output.path = resolveApp('../../target/web/public/main/lib/silk-workbench-core/libs/silk-legacy-ui/')

module.exports = merge(baseConfig, {
  mode: 'development',
});
