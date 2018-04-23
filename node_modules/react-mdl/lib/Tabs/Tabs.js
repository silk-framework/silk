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

var _Tab = require('./Tab');

var _Tab2 = _interopRequireDefault(_Tab);

var _TabBar = require('./TabBar');

var _TabBar2 = _interopRequireDefault(_TabBar);

var _mdlUpgrade = require('../utils/mdlUpgrade');

var _mdlUpgrade2 = _interopRequireDefault(_mdlUpgrade);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var TabPropType = function TabPropType(props, propName, componentName) {
    var prop = props[propName];
    return prop.type !== _Tab2.default && new Error('\'' + componentName + '\' only accepts \'Tab\' as children.');
};

var propTypes = {
    activeTab: _propTypes2.default.number,
    children: _propTypes2.default.oneOfType([TabPropType, _propTypes2.default.arrayOf(TabPropType)]),
    className: _propTypes2.default.string,
    onChange: _propTypes2.default.func,
    tabBarProps: _propTypes2.default.object,
    ripple: _propTypes2.default.bool
};

var Tabs = function Tabs(props) {
    var activeTab = props.activeTab,
        className = props.className,
        onChange = props.onChange,
        children = props.children,
        tabBarProps = props.tabBarProps,
        ripple = props.ripple,
        otherProps = _objectWithoutProperties(props, ['activeTab', 'className', 'onChange', 'children', 'tabBarProps', 'ripple']);

    var classes = (0, _classnames2.default)('mdl-tabs mdl-js-tabs', {
        'mdl-js-ripple-effect': ripple
    }, className);

    return _react2.default.createElement(
        'div',
        _extends({ className: classes }, otherProps),
        _react2.default.createElement(
            _TabBar2.default,
            _extends({ cssPrefix: 'mdl-tabs', activeTab: activeTab, onChange: onChange }, tabBarProps),
            children
        )
    );
};

Tabs.propTypes = propTypes;

exports.default = (0, _mdlUpgrade2.default)(Tabs, true);