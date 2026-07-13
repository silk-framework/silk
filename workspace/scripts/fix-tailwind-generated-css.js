/**
 * Post-processes the Tailwind-CLI output for the webpack-4 toolchain: the prod
 * CSS minifier (cssnano/postcss-calc) cannot parse the CSS-Values-4 expression
 * `calc(infinity * 1px)` that Tailwind v4 emits (e.g. for `rounded-full`) and
 * hard-fails the build. `9999px` is the classic pill-radius equivalent and
 * renders identically for the border-radius use cases Tailwind emits it for.
 */
const fs = require("fs");
const path = require("path");

const target = path.resolve(__dirname, "../src/theme/tailwind.generated.css");
const css = fs.readFileSync(target, "utf8");
const fixed = css.replace(/calc\(infinity \* 1px\)/g, "9999px");
if (fixed !== css) {
    fs.writeFileSync(target, fixed);
    console.log("[fix-tailwind-generated-css] replaced calc(infinity * 1px) with 9999px");
}
