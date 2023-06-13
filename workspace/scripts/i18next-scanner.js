const fs = require("fs");
/* eslint-disable import/no-extraneous-dependencies */
const _ = require("lodash");
const eol = require("eol");
const path = require("path");
const paths = require("../config/paths");
const VirtualFile = require("vinyl");
const vfs = require("vinyl-fs");
("use strict");
const TEMP_TARGET_LANG_FILE_DIR = "./target/locales";

const flattenObjectKeys = require("i18next-scanner/lib/flatten-object-keys").default;
const omitEmptyObject = require("i18next-scanner/lib/omit-empty-object").default;

const NOT_TRANSLATED = "__STRING_NOT_TRANSLATED__";

function getFileJSON(resPath, parentPath) {
    try {
        return JSON.parse(
            fs.readFileSync(fs.realpathSync(parentPath ? path.join("src", resPath) : resPath)).toString("utf-8")
        );
    } catch (e) {
        return {};
    }
}

function writeJsonFile(value, targetPath) {
    const json = JSON.stringify(value, null, 2);
    fs.writeFileSync(targetPath, json, "utf8");
}

// Checks env variable ADDITIONAL_LANGUAGE_FILES to contain a directory path to additional language files
// The directory must contain files in the format '<LANG_CODE>.json', e.g. 'en.json'.
function fetchAdditionalLanguageFiles() {
    const additionalFiles = paths.silkConfig.additionalLanguageFolder
        ? paths.silkConfig.additionalLanguageFolder
        : process.env.ADDITIONAL_LANGUAGE_FILES;
    if (additionalFiles) {
        const additional = additionalFiles.replace(/\/$/, "");
        return fs.readdirSync(additional).map((file) => `${additional}/${file}`);
    } else {
        return [];
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
                let resContent = getFileJSON(resPath, "src");

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

// Merge source object into target object recursively.
// The merge will result in an error if sub-structures cannot be merged, i.e. values for same key exist and are not both objects or both strings.
// If there are 2 conflicting string values for the same key, the value from the source is taken.
const deepMerge = (source, target) => {
    for (const key of new Set(Object.keys(target).concat(Object.keys(source)))) {
        if (source[key] !== undefined && target[key] !== undefined) {
            if(typeof source[key] === "string" || typeof target[key] === "string") {
                console.log(`Found 2 conflicting values for key '${key}'. Selected value: '${source[key]}'`)
                target[key] = source[key]
            } else if (typeof source[key] === "object" && typeof target[key] === "object") {
                Object.assign(target[key], deepMerge(target[key], source[key]));
            } else {
                throw Error(
                    `When merging values that are both in source and target, they need both to be objects or both to be strings. But value for key '${key}' was not.`
                );
            }
        } else if (source[key] !== undefined) {
            target[key] = source[key];
        }
    }

    // Join `target` and modified `source`
    Object.assign(source, target);
    return target;
};

// Merges all existing language files into one for each language
function createTempLanguageFiles(inputLanguageFiles) {
    const extractLang = /([a-z]{2,2})\.json/;
    const filesByLanguage = new Map();
    for (const inputLangFile of inputLanguageFiles) {
        const [input, lang] = extractLang.exec(inputLangFile);
        if (lang) {
            if (filesByLanguage.has(lang)) {
                filesByLanguage.get(lang).push(inputLangFile);
            } else {
                filesByLanguage.set(lang, [inputLangFile]);
            }
        }
    }
    for (const lang of filesByLanguage.keys()) {
        const files = filesByLanguage.get(lang);
        const result = {};
        for (const file of files) {
            const sourceJson = getFileJSON(file);
            deepMerge(sourceJson, result);
        }
        console.log("Writing temp language files: " + lang + ".json");
        writeJsonFile(result, TEMP_TARGET_LANG_FILE_DIR + "/" + lang + ".json");
    }
    return [...filesByLanguage.keys()];
}

function customTransform(file, enc, done) {
    done();
}

// Init temp locales dir
fs.rmSync(TEMP_TARGET_LANG_FILE_DIR, { recursive: true, force: true });
fs.mkdirSync(TEMP_TARGET_LANG_FILE_DIR, { recursive: true });
const manualLangFiles = fs.readdirSync("./src/locales/manual/").map((file) => "./src/locales/manual/" + file);
// Copy and merge lang files. String values for keys are overwritten by the additional lang files.
const additionalLangFiles = fetchAdditionalLanguageFiles();
const foundLanguages = createTempLanguageFiles(manualLangFiles.concat(additionalLangFiles));

function generateEmptyLanguageFiles() {
    fs.mkdirSync("src/locales/generated", { recursive: true });
    for (const lang of ["de", "en", "fr"]) {
        // If a language file was not available write an empty language file. This is needed for the code to compile.
        if (!foundLanguages.includes(lang)) {
            writeJsonFile({}, `src/locales/generated/${lang}.json`);
        }
    }
}

/** Find missing keys and list them. Return error code if there exist missing values. */
function validate() {
    const missingKeys = new Map();
    for (const lang of ["de", "en", "fr"]) {
        const missingKeySet = new Set();
        missingKeys.set(lang, missingKeySet);

        function checkForMissingValues(currentPath, obj) {
            if (obj === NOT_TRANSLATED) {
                missingKeySet.add(currentPath.join("."));
            } else if (typeof obj === "object") {
                Object.entries(obj).forEach(([key, value]) => checkForMissingValues([...currentPath, key], value));
            }
        }

        const langFileContent = getFileJSON(`src/locales/generated/${lang}.json`);
        checkForMissingValues([], langFileContent);
    }
    const languagesWithMissingKeys = [...missingKeys].filter(([lang, missingKeys]) => missingKeys.size > 0);
    if (languagesWithMissingKeys.length > 0) {
        console.warn("Missing translations found!");
        for (const [lang, missingKeys] of languagesWithMissingKeys) {
            console.warn(
                `For language '${lang}' ${missingKeys.size} keys do not have a translation value:\n  - ` +
                [...missingKeys].sort((a, b) => (a < b ? -1 : 1)).join("\n  - ")
            );
        }
        process.exit(1);
    }
}

const scanner = require("i18next-scanner");
vfs.src(
    ["src/app/**/*.{js,jsx,ts,tsx}", "!src/app/**/*.spec.{js,jsx,ts,tsx}", "!**/node_modules/**"].concat(
        paths.additionalSourcePaths().map((path) => `${path}/**/*.{js,jsx,ts,tsx}`)
    )
)
    .pipe(
        scanner(
            {
                debug: true,
                removeUnusedKeys: false,
                sort: true,
                attr: false,
                defaultLng: "en",
                fallbackKey: false,
                trans: false, //this is not working with typescript
                func: {
                    list: ["i18next.t", "i18n.t", "t"],
                    extensions: [".js", ".jsx", ".tsx", ".ts"],
                },
                resource: {
                    // the source path is relative to current working directory
                    loadPath: TEMP_TARGET_LANG_FILE_DIR + "/{{lng}}.json",
                    // The path to store resources. Relative to the path specified by `gulp.dest(path)`.
                    savePath: "src/locales/generated/{{lng}}.json",
                    // Specify the number of space characters to use as white space to insert into the output JSON string for readability purpose.
                    jsonIndent: 2,
                    // Normalize line endings to '\r\n', '\r', '\n', or 'auto' for the current operating system. Defaults to '\n'.
                    // Aliases: 'CRLF', 'CR', 'LF', 'crlf', 'cr', 'lf'
                    lineEnding: "\n",
                },
                lngs: foundLanguages,
                defaultValue: () => NOT_TRANSLATED,
            },
            customTransform,
            customFlush
        )
    )
    .pipe(vfs.dest("./"))
    .on("end", () => {
        generateEmptyLanguageFiles();
        validate();
    });
