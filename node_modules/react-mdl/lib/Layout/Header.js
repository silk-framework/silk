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

var _HeaderRow = require('./HeaderRow');

var _HeaderRow2 = _interopRequireDefault(_HeaderRow);

var _HeaderTabs = require('./HeaderTabs');

var _HeaderTabs2 = _interopRequireDefault(_HeaderTabs);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var Header = function Header(props) {
    var className = props.className,
        scroll = props.scroll,
        seamed = props.seamed,
        title = props.title,
        transparent = props.transparent,
        waterfall = props.waterfall,
        hideTop = props.hideTop,
        hideSpacer = props.hideSpacer,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'scroll', 'seamed', 'title', 'transparent', 'waterfall', 'hideTop', 'hideSpacer', 'children']);

    var classes = (0, _classnames2.default)('mdl-layout__header', {
        'mdl-layout__header--scroll': scroll,
        'mdl-layout__header--seamed': seamed,
        'mdl-layout__header--transparent': transparent,
        'mdl-layout__header--waterfall': waterfall,
        'mdl-layout__header--waterfall-hide-top': waterfall && hideTop
    }, className);

    var isRowOrTab = false;
    _react2.default.Children.forEach(children, function (child) {
        if (child && (child.type === _HeaderRow2.default || child.type === _HeaderTabs2.default)) {
            isRowOrTab = true;
        }
    });

    return _react2.default.createElement(
        'header',
        _extends({ className: classes }, otherProps),
        isRowOrTab ? children : _react2.default.createElement(
            _HeaderRow2.default,
            { title: title, hideSpacer: hideSpacer },
            children
        )
    );
};
Header.propTypes = {
    className: _propTypes2.default.string,
    scroll: _propTypes2.default.bool,
    seamed: _propTypes2.default.bool,
    title: _propTypes2.default.node,
    transparent: _propTypes2.default.bool,
    waterfall: _propTypes2.default.bool,
    hideTop: _propTypes2.default.bool,
    hideSpacer: _propTypes2.default.bool
};

exports.default = Header;