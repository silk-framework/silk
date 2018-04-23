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

var _cloneChildren = require('../utils/cloneChildren');

var _cloneChildren2 = _interopRequireDefault(_cloneChildren);

var _Spacer = require('./Spacer');

var _Spacer2 = _interopRequireDefault(_Spacer);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var Navigation = function Navigation(props) {
    var className = props.className,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'children']);

    var classes = (0, _classnames2.default)('mdl-navigation', className);

    return _react2.default.createElement(
        'nav',
        _extends({ className: classes }, otherProps),
        (0, _cloneChildren2.default)(children, function (child) {
            return {
                className: (0, _classnames2.default)({ 'mdl-navigation__link': child.type !== _Spacer2.default }, child.props.className)
            };
        })
    );
};
Navigation.propTypes = {
    className: _propTypes2.default.string
};

exports.default = Navigation;