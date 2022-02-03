/**
 * Build utility functions that are shared among multiple files.
 **/
const paths = require("../config/paths");

const adaptWebpackConfig = (config) => {
    let adaptedConf = {...config}
    if (paths.additionalSourcePaths().length > 0 || paths.additionalEntries().length > 0) {
        paths.additionalEntries().length > 0 && console.log("Add additional entry point/s: " + paths.additionalEntries().join(", "))
        adaptedConf.entry = adaptedConf.entry.concat(paths.additionalEntries())
        paths.additionalSourcePaths().length > 0 && adaptedConf.module.rules.forEach(rule => {
            if (Array.isArray(rule.include)) {
                console.log("Adding additional source path/s: " + paths.additionalSourcePaths().join(", "))
                rule.include = rule.include.concat(paths.additionalSourcePaths())
            }
            if (rule.oneOf) {
                rule.oneOf.forEach(oneOfRule => {
                    if (Array.isArray(oneOfRule.include)) {
                        console.log("Adding additional source path/s to module: " + oneOfRule.loader)
                        oneOfRule.include = oneOfRule.include.concat(paths.additionalSourcePaths())
                    }
                })
            }
        })
    }
    return adaptedConf
}

module.exports = {
    adaptWebpackConfig
}
