'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.Chip = exports.ChipText = exports.ChipContact = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _basicClassCreator = require('../utils/basicClassCreator');

var _basicClassCreator2 = _interopRequireDefault(_basicClassCreator);

var _Icon = require('../Icon');

var _Icon2 = _interopRequireDefault(_Icon);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    className: _propTypes2.default.string,
    onClick: _propTypes2.default.func,
    onClose: _propTypes2.default.func
};

var ChipContact = exports.ChipContact = (0, _basicClassCreator2.default)('ChipContact', 'mdl-chip__contact', 'span');
var ChipText = exports.ChipText = (0, _basicClassCreator2.default)('ChipText', 'mdl-chip__text', 'span');

var Chip = function Chip(props) {
    var className = props.className,
        onClick = props.onClick,
        onClose = props.onClose,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'onClick', 'onClose', 'children']);

    var childrenArray = _react2.default.Children.toArray(children);
    var contactIndex = childrenArray.findIndex(function (c) {
        return c.type === ChipContact;
    });

    var chipContent = [];

    if (contactIndex >= 0) {
        chipContent.push(childrenArray[contactIndex], _react2.default.createElement(
            ChipText,
            { key: 'text' },
            childrenArray.slice(0, contactIndex).concat(childrenArray.slice(contactIndex + 1))
        ));
    } else {
        chipContent.push(_react2.default.createElement(
            ChipText,
            { key: 'text' },
            children
        ));
    }

    if (onClose) {
        chipContent.push(_react2.default.createElement(
            'button',
            { key: 'btn', type: 'button', className: 'mdl-chip__action', onClick: onClose },
            _react2.default.createElement(_Icon2.default, { name: 'cancel' })
        ));
    }

    var elt = onClick ? 'button' : 'span';

    return _react2.default.createElement(elt, _extends({
        className: (0, _classnames2.default)('mdl-chip', {
            'mdl-chip--contact': contactIndex > -1,
            'mdl-chip--deletable': !!onClose
        }, className),
        type: onClick ? 'button' : null,
        onClick: onClick
    }, otherProps), chipContent);
};

exports.Chip = Chip;
Chip.propTypes = propTypes;