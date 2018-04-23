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

var _TabBar = require('../Tabs/TabBar');

var _TabBar2 = _interopRequireDefault(_TabBar);

var _mdlUpgrade = require('../utils/mdlUpgrade');

var _mdlUpgrade2 = _interopRequireDefault(_mdlUpgrade);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var HeaderTabs = function HeaderTabs(props) {
    var className = props.className,
        ripple = props.ripple,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'ripple', 'children']);

    var classes = (0, _classnames2.default)({
        'mdl-js-ripple-effect': ripple,
        'mdl-js-ripple-effect--ignore-events': ripple
    }, className);

    return _react2.default.createElement(
        _TabBar2.default,
        _extends({ cssPrefix: 'mdl-layout', className: classes }, otherProps),
        children
    );
};
HeaderTabs.propTypes = {
    activeTab: _propTypes2.default.number,
    className: _propTypes2.default.string,
    onChange: _propTypes2.default.func,
    ripple: _propTypes2.default.bool
};

exports.default = (0, _mdlUpgrade2.default)(HeaderTabs);