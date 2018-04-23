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

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

// This component doesn't use the javascript from MDL.
// This is the expected behavior and the reason is because it's not written in
// a way to make it easy to use with React.
var ANIMATION_LENGTH = 250;

var propTypes = {
    action: _propTypes2.default.string,
    active: _propTypes2.default.bool.isRequired,
    className: _propTypes2.default.string,
    onActionClick: _propTypes2.default.func,
    onTimeout: _propTypes2.default.func.isRequired,
    timeout: _propTypes2.default.number
};

var defaultProps = {
    timeout: 2750
};

var Snackbar = function (_React$Component) {
    _inherits(Snackbar, _React$Component);

    function Snackbar(props) {
        _classCallCheck(this, Snackbar);

        var _this = _possibleConstructorReturn(this, (Snackbar.__proto__ || Object.getPrototypeOf(Snackbar)).call(this, props));

        _this.clearTimer = _this.clearTimer.bind(_this);
        _this.timeoutId = null;
        _this.clearTimeoutId = null;
        _this.state = {
            open: false
        };
        return _this;
    }

    _createClass(Snackbar, [{
        key: 'componentWillReceiveProps',
        value: function componentWillReceiveProps(nextProps) {
            this.setState({
                open: nextProps.active
            });
        }
    }, {
        key: 'componentDidUpdate',
        value: function componentDidUpdate() {
            if (this.timeoutId) {
                clearTimeout(this.timeoutId);
            }

            if (this.props.active) {
                this.timeoutId = setTimeout(this.clearTimer, this.props.timeout);
            }
        }
    }, {
        key: 'componentWillUnmount',
        value: function componentWillUnmount() {
            if (this.timeoutId) {
                clearTimeout(this.timeoutId);
                this.timeoutId = null;
            }
            if (this.clearTimeoutId) {
                clearTimeout(this.clearTimeoutId);
                this.clearTimeoutId = null;
            }
        }
    }, {
        key: 'clearTimer',
        value: function clearTimer() {
            var _this2 = this;

            this.timeoutId = null;
            this.setState({ open: false });

            this.clearTimeoutId = setTimeout(function () {
                _this2.clearTimeoutId = null;
                _this2.props.onTimeout();
            }, ANIMATION_LENGTH);
        }
    }, {
        key: 'render',
        value: function render() {
            var _props = this.props,
                action = _props.action,
                active = _props.active,
                className = _props.className,
                children = _props.children,
                onActionClick = _props.onActionClick,
                otherProps = _objectWithoutProperties(_props, ['action', 'active', 'className', 'children', 'onActionClick']);

            var open = this.state.open;


            var classes = (0, _classnames2.default)('mdl-snackbar', {
                'mdl-snackbar--active': open
            }, className);

            delete otherProps.onTimeout;
            delete otherProps.timeout;

            return _react2.default.createElement(
                'div',
                _extends({ className: classes, 'aria-hidden': !open }, otherProps),
                _react2.default.createElement(
                    'div',
                    { className: 'mdl-snackbar__text' },
                    active && children
                ),
                active && action && _react2.default.createElement(
                    'button',
                    { className: 'mdl-snackbar__action', type: 'button', onClick: onActionClick },
                    action
                )
            );
        }
    }]);

    return Snackbar;
}(_react2.default.Component);

Snackbar.propTypes = propTypes;
Snackbar.defaultProps = defaultProps;

exports.default = Snackbar;