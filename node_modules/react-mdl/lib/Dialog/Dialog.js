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

var _reactDom = require('react-dom');

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    className: _propTypes2.default.string,
    onCancel: _propTypes2.default.func,
    onBackdropClick: _propTypes2.default.func,
    open: _propTypes2.default.bool
};

var defaultProps = {
    onCancel: function onCancel(e) {
        return e.preventDefault();
    }
};

var Dialog = function (_React$Component) {
    _inherits(Dialog, _React$Component);

    function Dialog() {
        _classCallCheck(this, Dialog);

        return _possibleConstructorReturn(this, (Dialog.__proto__ || Object.getPrototypeOf(Dialog)).apply(this, arguments));
    }

    _createClass(Dialog, [{
        key: 'componentDidMount',
        value: function componentDidMount() {
            this.backdropClickCallback = this.onDialogClick.bind(this);
            this.dialogRef.addEventListener('click', this.backdropClickCallback);
            this.dialogRef.addEventListener('cancel', this.props.onCancel);
            if (this.props.open) {
                (0, _reactDom.findDOMNode)(this).showModal();
            }
        }
    }, {
        key: 'componentDidUpdate',
        value: function componentDidUpdate(prevProps) {
            if (this.props.open !== prevProps.open) {
                if (this.props.open) {
                    (0, _reactDom.findDOMNode)(this).showModal();

                    // display the dialog at the right location
                    // needed for the polyfill, otherwise it's not at the right position
                    var windowHeight = window.innerHeight;
                    if (this.dialogRef) {
                        var dialogHeight = this.dialogRef.clientHeight;
                        this.dialogRef.style.position = 'fixed';
                        this.dialogRef.style.top = (windowHeight - dialogHeight) / 2 + 'px';
                    }
                } else {
                    (0, _reactDom.findDOMNode)(this).close();
                }
            }
        }
    }, {
        key: 'componentWillUnmount',
        value: function componentWillUnmount() {
            this.dialogRef.removeEventListener('cancel', this.props.onCancel);
            this.dialogRef.removeEventListener('click', this.backdropClickCallback);
        }
    }, {
        key: 'onDialogClick',
        value: function onDialogClick(event) {
            // http://stackoverflow.com/a/26984690
            if (this.props.onBackdropClick && event.target === this.dialogRef) {
                var rect = this.dialogRef.getBoundingClientRect();
                var insideDialog = rect.top <= event.clientY && event.clientY <= rect.top + rect.height && rect.left <= event.clientX && event.clientX <= rect.left + rect.width;

                if (!insideDialog) {
                    this.props.onBackdropClick();
                }
            }
        }
    }, {
        key: 'render',
        value: function render() {
            var _this2 = this;

            // We cannot set the `open` prop on the Dialog if we manage its state manually with `showModal`,
            // thus the disabled eslint rule
            // eslint-disable-next-line no-unused-vars
            var _props = this.props,
                className = _props.className,
                open = _props.open,
                onCancel = _props.onCancel,
                children = _props.children,
                onBackdropClick = _props.onBackdropClick,
                otherProps = _objectWithoutProperties(_props, ['className', 'open', 'onCancel', 'children', 'onBackdropClick']);

            var classes = (0, _classnames2.default)('mdl-dialog', className);

            return _react2.default.createElement(
                'dialog',
                _extends({ ref: function ref(c) {
                        return _this2.dialogRef = c;
                    }, className: classes }, otherProps),
                children
            );
        }
    }]);

    return Dialog;
}(_react2.default.Component);

Dialog.propTypes = propTypes;
Dialog.defaultProps = defaultProps;

exports.default = Dialog;