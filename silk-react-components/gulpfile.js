const gulp = require('ecc-gulp-tasks')(require('./buildConfig.js'));

gulp.task('default', ['debug']);
gulp.task('deploy', ['build-app']);
