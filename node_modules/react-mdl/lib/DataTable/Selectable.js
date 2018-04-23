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

var _lodash = require('lodash.isequal');

var _lodash2 = _interopRequireDefault(_lodash);

var _TableHeader = require('./TableHeader');

var _TableHeader2 = _interopRequireDefault(_TableHeader);

var _Checkbox = require('../Checkbox');

var _Checkbox2 = _interopRequireDefault(_Checkbox);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    columns: function columns(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use the component `TableHeader` instead.');
    },
    data: function data(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use `rows` instead. `' + propName + '` will be removed in the next major release.');
    },
    onSelectionChanged: _propTypes2.default.func,
    rowKeyColumn: _propTypes2.default.string,
    rows: _propTypes2.default.arrayOf(_propTypes2.default.object).isRequired,
    selectable: _propTypes2.default.bool,
    selectedRows: _propTypes2.default.array
};

var defaultProps = {
    onSelectionChanged: function onSelectionChanged() {
        // do nothing
    }
};

exports.default = function (Component) {
    var Selectable = function (_React$Component) {
        _inherits(Selectable, _React$Component);

        function Selectable(props) {
            _classCallCheck(this, Selectable);

            var _this = _possibleConstructorReturn(this, (Selectable.__proto__ || Object.getPrototypeOf(Selectable)).call(this, props));

            _this.handleChangeHeaderCheckbox = _this.handleChangeHeaderCheckbox.bind(_this);
            _this.handleChangeRowCheckbox = _this.handleChangeRowCheckbox.bind(_this);
            _this.builRowCheckbox = _this.builRowCheckbox.bind(_this);

            if (props.selectable) {
                _this.state = {
                    headerSelected: false,
                    selectedRows: props.selectedRows || []
                };
            }
            return _this;
        }

        _createClass(Selectable, [{
            key: 'componentWillReceiveProps',
            value: function componentWillReceiveProps(nextProps) {
                if (nextProps.selectable) {
                    var rows = nextProps.rows,
                        data = nextProps.data,
                        rowKeyColumn = nextProps.rowKeyColumn;

                    var rrows = rows || data;

                    if (!(0, _lodash2.default)(this.props.rows || this.props.data, rrows) || !(0, _lodash2.default)(this.props.selectedRows, nextProps.selectedRows)) {
                        // keep only existing rows
                        var selectedRows = (nextProps.selectedRows || this.state.selectedRows).filter(function (k) {
                            return rrows.map(function (row, i) {
                                return row[rowKeyColumn] || row.key || i;
                            }).indexOf(k) > -1;
                        });

                        this.setState({
                            headerSelected: selectedRows.length === rrows.length,
                            selectedRows: selectedRows
                        });

                        if (!nextProps.selectedRows) {
                            nextProps.onSelectionChanged(selectedRows);
                        }
                    }
                }
            }
        }, {
            key: 'handleChangeHeaderCheckbox',
            value: function handleChangeHeaderCheckbox(e) {
                var _props = this.props,
                    rowKeyColumn = _props.rowKeyColumn,
                    rows = _props.rows,
                    data = _props.data;

                var selected = e.target.checked;
                var selectedRows = selected ? (rows || data).map(function (row, idx) {
                    return row[rowKeyColumn] || row.key || idx;
                }) : [];

                this.setState({
                    headerSelected: selected,
                    selectedRows: selectedRows
                });

                this.props.onSelectionChanged(selectedRows);
            }
        }, {
            key: 'handleChangeRowCheckbox',
            value: function handleChangeRowCheckbox(e) {
                var _props2 = this.props,
                    rows = _props2.rows,
                    data = _props2.data;

                var rowId = JSON.parse(e.target.dataset ? e.target.dataset.reactmdl : e.target.getAttribute('data-reactmdl')).id;
                var rowChecked = e.target.checked;
                var selectedRows = this.state.selectedRows;

                if (rowChecked) {
                    selectedRows.push(rowId);
                } else {
                    var idx = selectedRows.indexOf(rowId);
                    selectedRows.splice(idx, 1);
                }

                this.setState({
                    headerSelected: (rows || data).length === selectedRows.length,
                    selectedRows: selectedRows
                });

                this.props.onSelectionChanged(selectedRows);
            }
        }, {
            key: 'builRowCheckbox',
            value: function builRowCheckbox(content, row, idx) {
                var rowKey = row[this.props.rowKeyColumn] || row.key || idx;
                var isSelected = this.state.selectedRows.indexOf(rowKey) > -1;
                return _react2.default.createElement(_Checkbox2.default, {
                    className: 'mdl-data-table__select',
                    'data-reactmdl': JSON.stringify({ id: rowKey }),
                    checked: isSelected,
                    onChange: this.handleChangeRowCheckbox
                });
            }
        }, {
            key: 'render',
            value: function render() {
                var _this2 = this;

                var _props3 = this.props,
                    rows = _props3.rows,
                    data = _props3.data,
                    selectable = _props3.selectable,
                    children = _props3.children,
                    rowKeyColumn = _props3.rowKeyColumn,
                    otherProps = _objectWithoutProperties(_props3, ['rows', 'data', 'selectable', 'children', 'rowKeyColumn']);

                // remove unwatned props
                // see https://github.com/Hacker0x01/react-datepicker/issues/517#issuecomment-230171426


                delete otherProps.onSelectionChanged;
                delete otherProps.selectedRows;

                var realRows = selectable ? (rows || data).map(function (row, idx) {
                    var rowKey = row[rowKeyColumn] || row.key || idx;
                    return _extends({}, row, {
                        className: (0, _classnames2.default)({
                            'is-selected': _this2.state.selectedRows.indexOf(rowKey) > -1
                        }, row.className)
                    });
                }) : rows || data;

                return _react2.default.createElement(
                    Component,
                    _extends({ rows: realRows }, otherProps),
                    selectable && _react2.default.createElement(
                        _TableHeader2.default,
                        { name: 'mdl-header-select', cellFormatter: this.builRowCheckbox },
                        _react2.default.createElement(_Checkbox2.default, {
                            className: 'mdl-data-table__select',
                            checked: this.state.headerSelected,
                            onChange: this.handleChangeHeaderCheckbox
                        })
                    ),
                    children
                );
            }
        }]);

        return Selectable;
    }(_react2.default.Component);

    Selectable.propTypes = propTypes;
    Selectable.defaultProps = defaultProps;
    return Selectable;
};