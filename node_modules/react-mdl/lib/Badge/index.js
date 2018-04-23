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
    children: _propTypes2.default.oneOfType([_propTypes2.default.element, _propTypes2.default.string]),
    className: _propTypes2.default.string,
    text: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]),
    overlap: _propTypes2.default.bool,
    noBackground: _propTypes2.default.bool
};

var Badge = function Badge(props) {
    var children = props.children,
        className = props.className,
        text = props.text,
        overlap = props.overlap,
        noBackground = props.noBackground,
        rest = _objectWithoutProperties(props, ['children', 'className', 'text', 'overlap', 'noBackground']);

    // No badge if no children
    // TODO: In React 15, we can return null instead


    if (!_react2.default.Children.count(children)) return _react2.default.createElement('noscript', null);

    var element = typeof children === 'string' ? _react2.default.createElement(
        'span',
        null,
        children
    ) : _react2.default.Children.only(children);

    // No text -> No need of badge
    if (text === null || typeof text === 'undefined') return element;

    return _react2.default.cloneElement(element, _extends({}, rest, {
        className: (0, _classnames2.default)(className, element.props.className, 'mdl-badge', {
            'mdl-badge--overlap': !!overlap,
            'mdl-badge--no-background': !!noBackground
        }),
        'data-badge': text
    }));
};

Badge.propTypes = propTypes;

exports.default = Badge;