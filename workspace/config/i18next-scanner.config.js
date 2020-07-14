const fs = require("fs");
const chalk = require("chalk");

module.exports = {
    input: ["src/app/**/*.{js,jsx,ts,tsx}", "!src/app/**/*.spec.{js,jsx,ts,tsx}", "!**/node_modules/**"],
    output: "src/locales",
    options: {
        debug: true,
        removeUnusedKeys: true,
        sort: true,
        attr: false,
        func: {
            list: ["i18next.t", "i18n.t", "t"],
            extensions: [".js", ".jsx", ".tsx", "ts"],
        },
        resource: {
            // The path to store resources. Relative to the path specified by `gulp.dest(path)`.
            savePath: "{{lng}}.json",
            // Specify the number of space characters to use as white space to insert into the output JSON string for readability purpose.
            jsonIndent: 2,
            // Normalize line endings to '\r\n', '\r', '\n', or 'auto' for the current operating system. Defaults to '\n'.
            // Aliases: 'CRLF', 'CR', 'LF', 'crlf', 'cr', 'lf'
            lineEnding: "\n",
        },
        lngs: ["en", "de"],
        defaultValue: "__STRING_NOT_TRANSLATED__",
    },
    transform: function customTransform(file, enc, done) {
        "use strict";
        const parser = this.parser;
        const content = fs.readFileSync(file.path, enc);
        let count = 0;

        parser.parseFuncFromString(content, { list: ["i18next._", "i18next.__"] }, (key, options) => {
            parser.set(
                key,
                Object.assign({}, options, {
                    nsSeparator: false,
                    keySeparator: false,
                })
            );
            ++count;
        });

        if (count > 0) {
            console.log(
                `i18next-scanner: count=${chalk.cyan(count)}, file=${chalk.yellow(JSON.stringify(file.relative))}`
            );
        }

        done();
    },
};
