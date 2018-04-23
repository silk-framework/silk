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

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    activeTab: _propTypes2.default.number,
    className: _propTypes2.default.string,
    cssPrefix: _propTypes2.default.string.isRequired,
    onChange: _propTypes2.default.func
};

var defaultProps = {
    activeTab: 0
};

var TabBar = function (_React$Component) {
    _inherits(TabBar, _React$Component);

    function TabBar(props) {
        _classCallCheck(this, TabBar);

        var _this = _possibleConstructorReturn(this, (TabBar.__proto__ || Object.getPrototypeOf(TabBar)).call(this, props));

        _this.handleClickTab = _this.handleClickTab.bind(_this);
        return _this;
    }

    _createClass(TabBar, [{
        key: 'handleClickTab',
        value: function handleClickTab(tabId) {
            if (this.props.onChange) {
                this.props.onChange(tabId);
            }
        }
    }, {
        key: 'render',
        value: function render() {
            var _this2 = this;

            var _props = this.props,
                activeTab = _props.activeTab,
                className = _props.className,
                cssPrefix = _props.cssPrefix,
                children = _props.children,
                otherProps = _objectWithoutProperties(_props, ['activeTab', 'className', 'cssPrefix', 'children']);

            var classes = (0, _classnames2.default)(_defineProperty({}, cssPrefix + '__tab-bar', true), className);

            return _react2.default.createElement(
                'div',
                _extends({ className: classes }, otherProps),
                _react2.default.Children.map(children, function (child, tabId) {
                    return _react2.default.cloneElement(child, {
                        cssPrefix: cssPrefix,
                        tabId: tabId,
                        active: tabId === activeTab,
                        onTabClick: _this2.handleClickTab
                    });
                })
            );
        }
    }]);

    return TabBar;
}(_react2.default.Component);

TabBar.propTypes = propTypes;
TabBar.defaultProps = defaultProps;

exports.default = TabBar;