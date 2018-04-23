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

var _Spacer = require('./Spacer');

var _Spacer2 = _interopRequireDefault(_Spacer);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var HeaderRow = function HeaderRow(props) {
    var className = props.className,
        title = props.title,
        children = props.children,
        hideSpacer = props.hideSpacer,
        otherProps = _objectWithoutProperties(props, ['className', 'title', 'children', 'hideSpacer']);

    var classes = (0, _classnames2.default)('mdl-layout__header-row', className);

    return _react2.default.createElement(
        'div',
        _extends({ className: classes }, otherProps),
        title && _react2.default.createElement(
            'span',
            { className: 'mdl-layout-title' },
            title
        ),
        title && !hideSpacer && _react2.default.createElement(_Spacer2.default, null),
        children
    );
};
HeaderRow.propTypes = {
    className: _propTypes2.default.string,
    title: _propTypes2.default.node,
    hideSpacer: _propTypes2.default.bool
};

exports.default = HeaderRow;