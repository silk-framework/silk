'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.MenuItem = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _reactDom = require('react-dom');

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _basicClassCreator = require('./utils/basicClassCreator');

var _basicClassCreator2 = _interopRequireDefault(_basicClassCreator);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    align: _propTypes2.default.oneOf(['left', 'right']),
    className: _propTypes2.default.string,
    ripple: _propTypes2.default.bool,
    target: _propTypes2.default.string.isRequired,
    valign: _propTypes2.default.oneOf(['bottom', 'top'])
};

var defaultProps = {
    align: 'left',
    valign: 'bottom'
};

// eslint-disable-next-line react/prefer-stateless-function

var Menu = function (_React$Component) {
    _inherits(Menu, _React$Component);

    function Menu() {
        _classCallCheck(this, Menu);

        return _possibleConstructorReturn(this, (Menu.__proto__ || Object.getPrototypeOf(Menu)).apply(this, arguments));
    }

    _createClass(Menu, [{
        key: 'componentDidMount',
        value: function componentDidMount() {
            window.componentHandler.upgradeElements((0, _reactDom.findDOMNode)(this));
        }
    }, {
        key: 'componentWillUnmount',
        value: function componentWillUnmount() {
            var elt = (0, _reactDom.findDOMNode)(this);

            window.componentHandler.downgradeElements(elt);

            var parent = elt.parentElement;
            var grandparent = parent && parent.parentElement;

            if (parent && grandparent && parent.classList.contains('mdl-menu__container')) {
                grandparent.replaceChild(elt, parent);
            }
        }
    }, {
        key: 'render',
        value: function render() {
            var _classNames;

            var _props = this.props,
                align = _props.align,
                children = _props.children,
                className = _props.className,
                ripple = _props.ripple,
                target = _props.target,
                valign = _props.valign,
                otherProps = _objectWithoutProperties(_props, ['align', 'children', 'className', 'ripple', 'target', 'valign']);

            var classes = (0, _classnames2.default)('mdl-menu mdl-js-menu', (_classNames = {}, _defineProperty(_classNames, 'mdl-menu--' + valign + '-' + align, true), _defineProperty(_classNames, 'mdl-js-ripple-effect', ripple), _classNames), className);

            return _react2.default.createElement(
                'ul',
                _extends({ className: classes, 'data-mdl-for': target }, otherProps),
                children
            );
        }
    }]);

    return Menu;
}(_react2.default.Component);

Menu.propTypes = propTypes;
Menu.defaultProps = defaultProps;

exports.default = Menu;
var MenuItem = exports.MenuItem = (0, _basicClassCreator2.default)('MenuItem', 'mdl-menu__item', 'li');