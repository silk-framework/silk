/**
 * Drop-in replacement for `zod/v3/helpers/errorUtil.js` (wired up via
 * NormalModuleReplacementPlugin in config/webpack.di.config.js).
 *
 * The original does `errorUtil.toString = …` — a plain assignment that throws
 * under this app's `Object.freeze(Object.prototype)` pollution defense
 * (src/index.tsx): strict mode rejects shadowing a non-writable inherited
 * property via assignment. `Object.defineProperty` creates the own property
 * directly and is unaffected. Semantics mirror the original exactly.
 */
export var errorUtil;
(function (errorUtil) {
    errorUtil.errToObj = (message) => (typeof message === "string" ? { message } : message || {});
    Object.defineProperty(errorUtil, "toString", {
        value: (message) =>
            typeof message === "string" ? message : message === null || message === undefined ? undefined : message.message,
        writable: true,
        enumerable: true,
        configurable: true,
    });
})(errorUtil || (errorUtil = {}));
