// Jest runs CommonJS tests. Transform the ESM-only dependencies reached by the shared UI,
// but leave ordinary node_modules packages out of Babel. Keep package boundaries explicit.
const esmPackages = [
    // Upload transport and its helpers.
    "@uppy[/\\\\][^/\\\\]+",
    "is-network-error",
    "nanoid",
    "p-retry",
    "pretty-bytes",

    // Markdown/HTML processing, including transitive unified dependencies.
    "@ungap[/\\\\]structured-clone",
    "@mavrin[/\\\\]remark-typograf",
    "react-markdown",
    "(?:rehype|remark)-[^/\\\\]+",
    "(?:hast|mdast|unist)-util-[^/\\\\]+",
    "micromark(?:-[^/\\\\]+)?",
    "vfile(?:-[^/\\\\]+)?",
    "bail",
    "ccount",
    "character-entities(?:-[^/\\\\]+)?",
    "character-reference-invalid",
    "comma-separated-tokens",
    "decode-named-character-reference",
    "devlop",
    "escape-string-regexp",
    "estree-util-is-identifier-name",
    "hastscript",
    "html-url-attributes",
    "html-void-elements",
    "is-absolute-url",
    "is-alphabetical",
    "is-alphanumerical",
    "is-decimal",
    "is-hexadecimal",
    "is-plain-obj",
    "longest-streak",
    "markdown-table",
    "parse-entities",
    "parse5",
    "property-information",
    "space-separated-tokens",
    "stringify-entities",
    "trim-lines",
    "trim-trailing-lines",
    "trough",
    "unified",
    "unist-builder",
    "web-namespaces",
    "zwitch",

    // Colors, graph interaction and Carbon's date/time support.
    "color(?:-convert|-name|-string)?",
    "d3-[^/\\\\]+",
    "classcat",
    "compute-scroll-into-view",
    "temporal-(?:polyfill|spec|utils)",
];

module.exports = {
    transformIgnorePatterns: [
        // Apply the allowlist to the innermost package, including nested dependency installations.
        `[/\\\\]node_modules[/\\\\](?!.*[/\\\\]node_modules[/\\\\])(?!(?:${esmPackages.join("|")})[/\\\\]).+\\.(?:js|jsx|ts|tsx|mjs|cjs)$`,
        "^.+\\.module\\.(css|sass|scss)$",
    ],
};
