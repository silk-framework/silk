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
    expand: _propTypes2.default.bool
};

var CardTitle = function CardTitle(props) {
    var className = props.className,
        children = props.children,
        expand = props.expand,
        otherProps = _objectWithoutProperties(props, ['className', 'children', 'expand']);

    var classes = (0, _classnames2.default)('mdl-card__title', {
        'mdl-card--expand': expand
    }, className);

    var title = typeof children === 'string' ? _react2.default.createElement(
        'h2',
        { className: 'mdl-card__title-text' },
        children
    ) : children;

    return _react2.default.createElement(
        'div',
        _extends({ className: classes }, otherProps),
        title
    );
};

CardTitle.propTypes = propTypes;

exports.default = CardTitle;