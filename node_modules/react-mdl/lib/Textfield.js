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

var _mdlUpgrade = require('./utils/mdlUpgrade');

var _mdlUpgrade2 = _interopRequireDefault(_mdlUpgrade);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    className: _propTypes2.default.string,
    disabled: _propTypes2.default.bool,
    error: _propTypes2.default.node,
    expandable: _propTypes2.default.bool,
    expandableIcon: _propTypes2.default.string,
    floatingLabel: _propTypes2.default.bool,
    id: function id(props, propName, componentName) {
        var id = props.id;

        if (id && typeof id !== 'string') {
            return new Error('Invalid prop `' + propName + '` supplied to `' + componentName + '`. `' + propName + '` should be a string. Validation failed.');
        }
        if (!id && typeof props.label !== 'string') {
            return new Error('Invalid prop `' + propName + '` supplied to `' + componentName + '`. `' + propName + '` is required when label is an element. Validation failed.');
        }
        return null;
    },
    inputClassName: _propTypes2.default.string,
    label: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.element]).isRequired,
    maxRows: _propTypes2.default.number,
    onChange: _propTypes2.default.func,
    pattern: _propTypes2.default.string,
    required: _propTypes2.default.bool,
    rows: _propTypes2.default.number,
    style: _propTypes2.default.object,
    value: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.number])
};

var Textfield = function (_React$Component) {
    _inherits(Textfield, _React$Component);

    function Textfield() {
        _classCallCheck(this, Textfield);

        return _possibleConstructorReturn(this, (Textfield.__proto__ || Object.getPrototypeOf(Textfield)).apply(this, arguments));
    }

    _createClass(Textfield, [{
        key: 'componentDidMount',
        value: function componentDidMount() {
            if (this.props.error && !this.props.pattern) {
                this.setAsInvalid();
            }
        }
    }, {
        key: 'componentDidUpdate',
        value: function componentDidUpdate(prevProps) {
            if (this.props.required !== prevProps.required || this.props.pattern !== prevProps.pattern || this.props.error !== prevProps.error) {
                (0, _reactDom.findDOMNode)(this).MaterialTextfield.checkValidity();
            }
            if (this.props.disabled !== prevProps.disabled) {
                (0, _reactDom.findDOMNode)(this).MaterialTextfield.checkDisabled();
            }
            if (this.props.value !== prevProps.value && this.inputRef !== document.activeElement) {
                (0, _reactDom.findDOMNode)(this).MaterialTextfield.change(this.props.value);
            }
            if (this.props.error && !this.props.pattern) {
                // Every time the input gets updated by MDL (checkValidity() or change())
                // its invalid class gets reset. We have to put it again if the input is specifically set as "invalid"
                this.setAsInvalid();
            }
        }
    }, {
        key: 'setAsInvalid',
        value: function setAsInvalid() {
            var elt = (0, _reactDom.findDOMNode)(this);
            if (elt.className.indexOf('is-invalid') < 0) {
                elt.className = (0, _classnames2.default)(elt.className, 'is-invalid');
            }
        }
    }, {
        key: 'render',
        value: function render() {
            var _this2 = this;

            var _props = this.props,
                className = _props.className,
                inputClassName = _props.inputClassName,
                id = _props.id,
                error = _props.error,
                expandable = _props.expandable,
                expandableIcon = _props.expandableIcon,
                floatingLabel = _props.floatingLabel,
                label = _props.label,
                maxRows = _props.maxRows,
                rows = _props.rows,
                style = _props.style,
                children = _props.children,
                otherProps = _objectWithoutProperties(_props, ['className', 'inputClassName', 'id', 'error', 'expandable', 'expandableIcon', 'floatingLabel', 'label', 'maxRows', 'rows', 'style', 'children']);

            var hasRows = !!rows;
            var customId = id || 'textfield-' + label.replace(/[^a-z0-9]/gi, '');
            var inputTag = hasRows || maxRows > 1 ? 'textarea' : 'input';

            var inputProps = _extends({
                className: (0, _classnames2.default)('mdl-textfield__input', inputClassName),
                id: customId,
                rows: rows,
                ref: function ref(c) {
                    return _this2.inputRef = c;
                }
            }, otherProps);

            var input = _react2.default.createElement(inputTag, inputProps);
            var labelContainer = _react2.default.createElement(
                'label',
                { className: 'mdl-textfield__label', htmlFor: customId },
                label
            );
            var errorContainer = !!error && _react2.default.createElement(
                'span',
                { className: 'mdl-textfield__error' },
                error
            );

            var containerClasses = (0, _classnames2.default)('mdl-textfield mdl-js-textfield', {
                'mdl-textfield--floating-label': floatingLabel,
                'mdl-textfield--expandable': expandable
            }, className);

            return expandable ? _react2.default.createElement(
                'div',
                { className: containerClasses, style: style },
                _react2.default.createElement(
                    'label',
                    { className: 'mdl-button mdl-js-button mdl-button--icon', htmlFor: customId },
                    _react2.default.createElement(
                        'i',
                        { className: 'material-icons' },
                        expandableIcon
                    )
                ),
                _react2.default.createElement(
                    'div',
                    { className: 'mdl-textfield__expandable-holder' },
                    input,
                    labelContainer,
                    errorContainer
                ),
                children
            ) : _react2.default.createElement(
                'div',
                { className: containerClasses, style: style },
                input,
                labelContainer,
                errorContainer,
                children
            );
        }
    }]);

    return Textfield;
}(_react2.default.Component);

Textfield.propTypes = propTypes;

exports.default = (0, _mdlUpgrade2.default)(Textfield);