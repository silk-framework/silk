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

var _ListItemContent = require('./ListItemContent');

var _ListItemContent2 = _interopRequireDefault(_ListItemContent);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    twoLine: _propTypes2.default.bool,
    threeLine: _propTypes2.default.bool
};

var ListItem = function ListItem(props) {
    var className = props.className,
        twoLine = props.twoLine,
        threeLine = props.threeLine,
        otherProps = _objectWithoutProperties(props, ['className', 'twoLine', 'threeLine']);

    var classes = (0, _classnames2.default)('mdl-list__item', {
        'mdl-list__item--two-line': twoLine && !threeLine,
        'mdl-list__item--three-line': !twoLine && threeLine
    }, className);

    var children = _react.Children.map(otherProps.children, function (child) {
        if (typeof child === 'string') {
            return _react2.default.createElement(
                _ListItemContent2.default,
                null,
                child
            );
        }
        if (child.type === _ListItemContent2.default) {
            return (0, _react.cloneElement)(child, {
                useBodyClass: !!threeLine
            });
        }
        return child;
    });

    return _react2.default.createElement(
        'li',
        _extends({ className: classes }, otherProps),
        children
    );
};

ListItem.propTypes = propTypes;

exports.default = ListItem;