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

var _clamp = require('clamp');

var _clamp2 = _interopRequireDefault(_clamp);

var _shadows = require('../utils/shadows');

var _shadows2 = _interopRequireDefault(_shadows);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    className: _propTypes2.default.string,
    shadow: _propTypes2.default.number
};

var Card = function Card(props) {
    var className = props.className,
        shadow = props.shadow,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'shadow', 'children']);

    var hasShadow = typeof shadow !== 'undefined';
    var shadowLevel = (0, _clamp2.default)(shadow || 0, 0, _shadows2.default.length - 1);

    var classes = (0, _classnames2.default)('mdl-card', _defineProperty({}, _shadows2.default[shadowLevel], hasShadow), className);

    return _react2.default.createElement(
        'div',
        _extends({ className: classes }, otherProps),
        children
    );
};

Card.propTypes = propTypes;

exports.default = Card;