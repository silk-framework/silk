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

var _MDLComponent = require('./utils/MDLComponent');

var _MDLComponent2 = _interopRequireDefault(_MDLComponent);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var Tooltip = function Tooltip(props) {
    var label = props.label,
        large = props.large,
        children = props.children,
        position = props.position,
        otherProps = _objectWithoutProperties(props, ['label', 'large', 'children', 'position']);

    var id = Math.random().toString(36).substr(2);

    var newLabel = typeof label === 'string' ? _react2.default.createElement(
        'span',
        null,
        label
    ) : label;

    var element = void 0;
    if (typeof children === 'string') {
        element = _react2.default.createElement(
            'span',
            null,
            children
        );
    } else {
        element = _react2.default.Children.only(children);
    }

    return _react2.default.createElement(
        'div',
        _extends({ style: { display: 'inline-block' } }, otherProps),
        _react2.default.cloneElement(element, { id: id }),
        _react2.default.createElement(
            _MDLComponent2.default,
            null,
            _react2.default.cloneElement(newLabel, {
                htmlFor: id,
                className: (0, _classnames2.default)('mdl-tooltip', _defineProperty({
                    'mdl-tooltip--large': large
                }, 'mdl-tooltip--' + position, typeof position !== 'undefined'))
            })
        )
    );
};

Tooltip.propTypes = {
    children: _propTypes2.default.node.isRequired,
    label: _propTypes2.default.node.isRequired,
    large: _propTypes2.default.bool,
    position: _propTypes2.default.oneOf(['left', 'right', 'top', 'bottom'])
};

exports.default = Tooltip;