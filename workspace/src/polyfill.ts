import entries from "core-js/features/object/entries";
import values from "core-js/features/object/values";
import arrayIncludes from "core-js/features/array/includes";
import find from "core-js/features/array/find";
import Promise from "core-js-pure/features/promise";

if (!Array.find) {
    Array.find = find;
}
if (!Object.values) {
    Object.values = values;
}
if (!Object.entries) {
    Object.entries = entries;
}
if (!Array.includes) {
    Array.includes = arrayIncludes;
}
if (!window.Promise) {
    window.Promise = Promise;
}
