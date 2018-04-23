'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

exports.default = function (children, props) {
    return _react2.default.Children.map(children, function (child) {
        if (!child) return child;
        var newProps = typeof props === 'function' ? props(child) : props;
        return _react2.default.cloneElement(child, newProps);
    });
};