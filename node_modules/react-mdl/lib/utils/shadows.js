"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
var values = [2, 3, 4, 6, 8, 16, 24];
exports.default = values.map(function (v) {
  return "mdl-shadow--" + v + "dp";
});