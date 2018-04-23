'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _Radio = require('./Radio');

var _Radio2 = _interopRequireDefault(_Radio);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var RadioGroup = function RadioGroup(props) {
    var name = props.name,
        value = props.value,
        children = props.children,
        container = props.container,
        childContainer = props.childContainer,
        onChange = props.onChange,
        otherProps = _objectWithoutProperties(props, ['name', 'value', 'children', 'container', 'childContainer', 'onChange']);

    var hasOnChange = typeof onChange === 'function';
    var checked = hasOnChange ? 'checked' : 'defaultChecked';

    return _react2.default.createElement(container, otherProps, _react2.default.Children.map(children, function (child) {
        var _extends2;

        var clonedChild = _react2.default.cloneElement(child, _extends((_extends2 = {}, _defineProperty(_extends2, checked, child.props.value === value), _defineProperty(_extends2, 'name', name), _defineProperty(_extends2, 'onChange', onChange), _extends2), otherProps));

        return childContainer ? _react2.default.createElement(childContainer, {}, clonedChild) : clonedChild;
    }));
};

RadioGroup.propTypes = {
    childContainer: _propTypes2.default.string,
    children: _propTypes2.default.arrayOf(function (props, propName, componentName) {
        var prop = props[propName];
        return prop.type !== _Radio2.default && new Error('\'' + componentName + '\' only accepts \'Radio\' as children.');
    }),
    container: _propTypes2.default.string,
    name: _propTypes2.default.string.isRequired,
    onChange: _propTypes2.default.func,
    value: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number]).isRequired
};

RadioGroup.defaultProps = {
    container: 'div'
};

exports.default = RadioGroup;