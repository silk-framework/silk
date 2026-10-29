const fs = require("fs");
const path = require("path");
const chalk = require("react-dev-utils/chalk");

function checkRequiredFiles(files) {
    const missingFile = files.find((file) => !fs.existsSync(file));
    if (!missingFile) {
        return true;
    }

    console.log(chalk.red("Could not find a required file."));
    console.log(chalk.red("  Name: ") + chalk.cyan(path.basename(missingFile)));
    console.log(chalk.red("  Searched in: ") + chalk.cyan(path.dirname(missingFile)));
    return false;
}

module.exports = checkRequiredFiles;
