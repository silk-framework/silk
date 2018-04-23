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
    buffer: _propTypes2.default.number,
    className: _propTypes2.default.string,
    indeterminate: _propTypes2.default.bool,
    progress: _propTypes2.default.number
};

var ProgressBar = function (_React$Component) {
    _inherits(ProgressBar, _React$Component);

    function ProgressBar() {
        _classCallCheck(this, ProgressBar);

        return _possibleConstructorReturn(this, (ProgressBar.__proto__ || Object.getPrototypeOf(ProgressBar)).apply(this, arguments));
    }

    _createClass(ProgressBar, [{
        key: 'componentDidMount',
        value: function componentDidMount() {
            this.setProgress(this.props.progress);
            this.setBuffer(this.props.buffer);
        }
    }, {
        key: 'componentDidUpdate',
        value: function componentDidUpdate() {
            this.setProgress(this.props.progress);
            this.setBuffer(this.props.buffer);
        }
    }, {
        key: 'setProgress',
        value: function setProgress(progress) {
            if (!this.props.indeterminate && progress !== undefined) {
                (0, _reactDom.findDOMNode)(this).MaterialProgress.setProgress(progress);
            }
        }
    }, {
        key: 'setBuffer',
        value: function setBuffer(buffer) {
            if (buffer !== undefined) {
                (0, _reactDom.findDOMNode)(this).MaterialProgress.setBuffer(buffer);
            }
        }
    }, {
        key: 'render',
        value: function render() {
            var _props = this.props,
                className = _props.className,
                indeterminate = _props.indeterminate,
                buffer = _props.buffer,
                progress = _props.progress,
                otherProps = _objectWithoutProperties(_props, ['className', 'indeterminate', 'buffer', 'progress']);

            var classes = (0, _classnames2.default)('mdl-progress mdl-js-progress', {
                'mdl-progress__indeterminate': indeterminate
            }, className);

            return _react2.default.createElement('div', _extends({ className: classes }, otherProps));
        }
    }]);

    return ProgressBar;
}(_react2.default.Component);

ProgressBar.propTypes = propTypes;

exports.default = (0, _mdlUpgrade2.default)(ProgressBar);