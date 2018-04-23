'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _mdlUpgrade = require('./utils/mdlUpgrade');

var _mdlUpgrade2 = _interopRequireDefault(_mdlUpgrade);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    accent: _propTypes2.default.bool,
    className: _propTypes2.default.string,
    colored: _propTypes2.default.bool,
    component: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.element, _propTypes2.default.func]),
    href: _propTypes2.default.string,
    primary: _propTypes2.default.bool,
    raised: _propTypes2.default.bool,
    ripple: _propTypes2.default.bool
};

// eslint-disable-next-line react/prefer-stateless-function

var Button = function (_React$Component) {
    _inherits(Button, _React$Component);

    function Button() {
        _classCallCheck(this, Button);

        return _possibleConstructorReturn(this, (Button.__proto__ || Object.getPrototypeOf(Button)).apply(this, arguments));
    }

    _createClass(Button, [{
        key: 'render',
        value: function render() {
            var _props = this.props,
                accent = _props.accent,
                className = _props.className,
                colored = _props.colored,
                primary = _props.primary,
                raised = _props.raised,
                ripple = _props.ripple,
                component = _props.component,
                href = _props.href,
                children = _props.children,
                otherProps = _objectWithoutProperties(_props, ['accent', 'className', 'colored', 'primary', 'raised', 'ripple', 'component', 'href', 'children']);

            var buttonClasses = (0, _classnames2.default)('mdl-button mdl-js-button', {
                'mdl-js-ripple-effect': ripple,
                'mdl-button--raised': raised,
                'mdl-button--colored': colored,
                'mdl-button--primary': primary,
                'mdl-button--accent': accent
            }, className);

            return _react2.default.createElement(component || (href ? 'a' : 'button'), _extends({
                className: buttonClasses,
                href: href
            }, otherProps), children);
        }
    }]);

    return Button;
}(_react2.default.Component);

Button.propTypes = propTypes;

exports.default = (0, _mdlUpgrade2.default)(Button);