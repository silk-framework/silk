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

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    active: _propTypes2.default.bool,
    className: _propTypes2.default.string,
    component: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.element, _propTypes2.default.func]),
    cssPrefix: _propTypes2.default.string,
    onTabClick: _propTypes2.default.func,
    style: _propTypes2.default.object,
    tabId: _propTypes2.default.number
};

var defaultProps = {
    style: {}
};

var Tab = function Tab(props) {
    var _classNames;

    var active = props.active,
        className = props.className,
        component = props.component,
        children = props.children,
        cssPrefix = props.cssPrefix,
        onTabClick = props.onTabClick,
        style = props.style,
        tabId = props.tabId,
        otherProps = _objectWithoutProperties(props, ['active', 'className', 'component', 'children', 'cssPrefix', 'onTabClick', 'style', 'tabId']);

    var classes = (0, _classnames2.default)((_classNames = {}, _defineProperty(_classNames, cssPrefix + '__tab', true), _defineProperty(_classNames, 'is-active', active), _classNames), className);

    var finalStyle = _extends({}, style, { cursor: 'pointer' });

    return _react2.default.createElement(component || 'a', _extends({
        className: classes,
        onClick: function onClick() {
            return onTabClick(tabId);
        },
        style: finalStyle
    }, otherProps), children);
};

Tab.propTypes = propTypes;
Tab.defaultProps = defaultProps;

exports.default = Tab;