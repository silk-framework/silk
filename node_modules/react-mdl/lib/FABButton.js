'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _Button = require('./Button');

var _Button2 = _interopRequireDefault(_Button);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var FABButton = function FABButton(props) {
    var mini = props.mini,
        className = props.className,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['mini', 'className', 'children']);

    var classes = (0, _classnames2.default)('mdl-button--fab', {
        'mdl-button--mini-fab': mini
    }, className);

    return _react2.default.createElement(
        _Button2.default,
        _extends({ className: classes }, otherProps),
        children
    );
};

FABButton.propTypes = {
    className: _propTypes2.default.string,
    mini: _propTypes2.default.bool
};

exports.default = FABButton;