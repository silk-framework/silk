'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.getColorClass = getColorClass;
exports.getTextColorClass = getTextColorClass;
// see https://github.com/google/material-design-lite/blob/master/src/palette/_palette.scss
// for the color and level possibilities

function getColorClass(color, level) {
    var lvlClass = level ? '-' + level : '';
    return 'mdl-color--' + color + lvlClass;
}

function getTextColorClass(color, level) {
    var lvlClass = level ? '-' + level : '';
    return 'mdl-color-text--' + color + lvlClass;
}