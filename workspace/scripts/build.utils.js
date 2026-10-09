/**
 * Build utility functions that are shared among multiple files.
 **/
const setBuildFailureExitCode = (isWatch, runtimeProcess = process) => {
    if (!isWatch) {
        runtimeProcess.exitCode = 1;
    }
};

const runCompiler = (compiler) =>
    new Promise((resolve, reject) => {
        compiler.run((compilationError, stats) => {
            compiler.close((closeError) => {
                if (compilationError) {
                    reject(compilationError);
                } else if (closeError) {
                    reject(closeError);
                } else {
                    resolve(stats);
                }
            });
        });
    });

module.exports = {
    runCompiler,
    setBuildFailureExitCode,
};
