/**
 * Post-processes the Tailwind-CLI output for the webpack-4 toolchain: the prod
 * CSS minifier (cssnano/postcss-calc) cannot parse some CSS-Values-4 expressions
 * Tailwind v4 emits and hard-fails the build.
 *
 * - `calc(infinity * 1px)` (e.g. for `rounded-full`): `9999px` is the classic
 *   pill-radius equivalent and renders identically for the border-radius use
 *   cases Tailwind emits it for.
 * - `var(--x, calc(var(--spacing) * N))` (nested calc inside a var() fallback,
 *   e.g. the scroll-fade animation ranges): the fallback is resolved against
 *   the base spacing token (--spacing: 0.25rem) to a rem literal. Only the
 *   fallback value is frozen; explicitly set --x values keep working.
 */
const fs = require("fs");
const path = require("path");

const target = path.resolve(__dirname, "../src/theme/tailwind.generated.css");
const css = fs.readFileSync(target, "utf8");
let fixed = css.replace(/calc\(infinity \* 1px\)/g, "9999px");
fixed = fixed.replace(
    /var\((--[\w-]+), calc\(var\(--spacing\) \* (\d+(?:\.\d+)?)\)\)/g,
    (_match, name, factor) => `var(${name}, ${Number(factor) * 0.25}rem)`,
);
if (fixed !== css) {
    fs.writeFileSync(target, fixed);
    console.log("[fix-tailwind-generated-css] rewrote postcss-calc-incompatible expressions");
}
