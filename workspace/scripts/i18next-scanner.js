const fs = require("fs");
const chalk = require("chalk");
/* eslint-disable import/no-extraneous-dependencies */
const _ = require("lodash");
const eol = require("eol");
const path = require("path");
const VirtualFile = require("vinyl");
const vfs = require("vinyl-fs");
("use strict");

const flattenObjectKeys = require("i18next-scanner/lib/flatten-object-keys").default;
const omitEmptyObject = require("i18next-scanner/lib/omit-empty-object").default;

function getFileJSON(resPath) {
    try {
        return JSON.parse(fs.readFileSync(fs.realpathSync(path.join("src", resPath))).toString("utf-8"));
    } catch (e) {
        return {};
    }
}

function customFlush(done) {
    const { parser } = this;
    const { options } = parser;

    // Flush to resource store
    const resStore = parser.get({ sort: options.sort });
    const { jsonIndent } = options.resource;
    const lineEnding = String(options.resource.lineEnding).toLowerCase();

    Object.keys(resStore).forEach((lng) => {
        const namespaces = resStore[lng];

        Object.keys(namespaces).forEach((ns) => {
            let obj = namespaces[ns];

            const resPath = parser.formatResourceSavePath(lng, ns);

            // if not defaultLng then Get, Merge & removeUnusedKeys of old JSON content
            if (lng !== options.defaultLng) {
                let resContent = getFileJSON(resPath);

                if (options.removeUnusedKeys) {
                    const namespaceKeys = flattenObjectKeys(obj);
                    const resContentKeys = flattenObjectKeys(resContent);
                    const unusedKeys = _.differenceWith(resContentKeys, namespaceKeys, _.isEqual);

                    for (let i = 0; i < unusedKeys.length; ++i) {
                        _.unset(resContent, unusedKeys[i]);
                    }

                    resContent = omitEmptyObject(resContent);
                }

                obj = { ...obj, ...resContent };
            }

            let text = `${JSON.stringify(obj, null, jsonIndent)}\n`;

            if (lineEnding === "auto") {
                text = eol.auto(text);
            } else if (lineEnding === "\r\n" || lineEnding === "crlf") {
                text = eol.crlf(text);
            } else if (lineEnding === "\n" || lineEnding === "lf") {
                text = eol.lf(text);
            } else if (lineEnding === "\r" || lineEnding === "cr") {
                text = eol.cr(text);
            } else {
                // Defaults to LF
                text = eol.lf(text);
            }
            this.push(
                new VirtualFile({
                    path: resPath,
                    contents: Buffer.from(text),
                })
            );
        });
    });

    done();
}

function customTransform(file, enc, done) {
    const parser = this.parser;
    const content = fs.readFileSync(file.path, enc);

    parser.parseFuncFromString(content, { list: ["i18next._"] }, (key, options) => {
        if (parser.get(key) === "__STRING_NOT_TRANSLATED__") {
        }
        // console.log(file.path, key);
        // parser.set(
        //     key,
        //     Object.assign({}, options, {
        //         nsSeparator: false,
        //         keySeparator: false,
        //     })
        // );
    });
    done();
}

const scanner = require("i18next-scanner");
vfs.src(["src/app/**/*.{js,jsx,ts,tsx}", "!src/app/**/*.spec.{js,jsx,ts,tsx}", "!**/node_modules/**"])
    .pipe(
        scanner(
            {
                debug: true,
                removeUnusedKeys: true,
                sort: true,
                attr: false,
                defaultLng: "en",
                trans: false, //this is not work with typescript
                func: {
                    list: ["i18next.t", "i18n.t", "t"],
                    extensions: [".js", ".jsx", ".tsx", "ts"],
                },
                resource: {
                    // the source path is relative to current working directory
                    loadPath: "src/locales/icu/{{lng}}.json",
                    // The path to store resources. Relative to the path specified by `gulp.dest(path)`.
                    savePath: "src/locales/{{lng}}.json",
                    // Specify the number of space characters to use as white space to insert into the output JSON string for readability purpose.
                    jsonIndent: 2,
                    // Normalize line endings to '\r\n', '\r', '\n', or 'auto' for the current operating system. Defaults to '\n'.
                    // Aliases: 'CRLF', 'CR', 'LF', 'crlf', 'cr', 'lf'
                    lineEnding: "\n",
                },
                lngs: ["en", "de"],
                defaultValue: "__STRING_NOT_TRANSLATED__",
            },
            customTransform,
            customFlush
        )
    )
    .pipe(vfs.dest("./"));
