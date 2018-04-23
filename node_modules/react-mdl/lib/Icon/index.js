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

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    className: _propTypes2.default.string,
    name: _propTypes2.default.string.isRequired
};

var Icon = function Icon(props) {
    var className = props.className,
        name = props.name,
        otherProps = _objectWithoutProperties(props, ['className', 'name']);

    var classes = (0, _classnames2.default)('material-icons', className);

    return _react2.default.createElement(
        'i',
        _extends({ className: classes }, otherProps),
        name
    );
};

Icon.propTypes = propTypes;

exports.default = Icon;