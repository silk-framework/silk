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

var _TableHeader = require('./TableHeader');

var _TableHeader2 = _interopRequireDefault(_TableHeader);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

function initState(props) {
    return {
        rows: (props.rows || props.data).slice(),
        sortHeader: null,
        isAsc: true
    };
}

var propTypes = {
    columns: function columns(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use the component `TableHeader` instead.');
    },
    data: function data(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use `rows` instead. `' + propName + '` will be removed in the next major release.');
    },
    rows: _propTypes2.default.arrayOf(_propTypes2.default.object).isRequired,
    sortable: _propTypes2.default.bool
};

exports.default = function (Component) {
    var Sortable = function (_React$Component) {
        _inherits(Sortable, _React$Component);

        function Sortable(props) {
            _classCallCheck(this, Sortable);

            var _this = _possibleConstructorReturn(this, (Sortable.__proto__ || Object.getPrototypeOf(Sortable)).call(this, props));

            _this.handleClickColumn = _this.handleClickColumn.bind(_this);

            if (props.sortable) {
                _this.state = initState(props);
            }
            return _this;
        }

        _createClass(Sortable, [{
            key: 'componentWillReceiveProps',
            value: function componentWillReceiveProps(nextProps) {
                if (nextProps.sortable) {
                    var realRows = nextProps.rows || nextProps.data;
                    var rows = this.state.sortHeader ? this.getSortedRowsForColumn(this.state.isAsc, this.state.sortHeader, realRows) : realRows;

                    this.setState({
                        rows: rows
                    });
                }
            }
        }, {
            key: 'getColumnClass',
            value: function getColumnClass(column) {
                var _state = this.state,
                    sortHeader = _state.sortHeader,
                    isAsc = _state.isAsc;


                return (0, _classnames2.default)(column.className, {
                    'mdl-data-table__header--sorted-ascending': sortHeader === column.name && isAsc,
                    'mdl-data-table__header--sorted-descending': sortHeader === column.name && !isAsc
                });
            }
        }, {
            key: 'getDefaultSortFn',
            value: function getDefaultSortFn(a, b, isAsc) {
                return isAsc ? a.localeCompare(b) : b.localeCompare(a);
            }
        }, {
            key: 'getSortedRowsForColumn',
            value: function getSortedRowsForColumn(isAsc, columnName, rows) {
                var columns = !!this.props.children ? _react2.default.Children.map(this.props.children, function (child) {
                    return child.props;
                }) : this.props.columns;

                var sortFn = this.getDefaultSortFn;
                for (var i = 0; i < columns.length; i++) {
                    if (columns[i].name === columnName && columns[i].sortFn) {
                        sortFn = columns[i].sortFn;
                        break;
                    }
                }

                return rows.sort(function (a, b) {
                    return sortFn(String(a[columnName]), String(b[columnName]), isAsc);
                });
            }
        }, {
            key: 'handleClickColumn',
            value: function handleClickColumn(e, columnName) {
                var isAsc = this.state.sortHeader === columnName ? !this.state.isAsc : true;
                var rows = this.getSortedRowsForColumn(isAsc, columnName, this.state.rows);
                this.setState({
                    sortHeader: columnName,
                    isAsc: isAsc,
                    rows: rows
                });
            }
        }, {
            key: 'renderTableHeaders',
            value: function renderTableHeaders() {
                var _this2 = this;

                var _props = this.props,
                    children = _props.children,
                    columns = _props.columns,
                    sortable = _props.sortable;


                if (sortable) {
                    return children ? _react2.default.Children.map(children, function (child) {
                        return _react2.default.cloneElement(child, {
                            className: _this2.getColumnClass(child.props),
                            onClick: _this2.handleClickColumn
                        });
                    }) : columns.map(function (column) {
                        return _react2.default.createElement(
                            _TableHeader2.default,
                            {
                                key: column.name,
                                className: _this2.getColumnClass(column),
                                name: column.name,
                                numeric: column.numeric,
                                tooltip: column.tooltip,
                                onClick: _this2.handleClickColumn
                            },
                            column.label
                        );
                    });
                }
                return children;
            }
        }, {
            key: 'render',
            value: function render() {
                var _props2 = this.props,
                    rows = _props2.rows,
                    data = _props2.data,
                    otherProps = _objectWithoutProperties(_props2, ['rows', 'data']);

                var realRows = this.state && this.state.rows || rows || data;

                // remove unwanted props
                delete otherProps.sortable;

                return _react2.default.createElement(
                    Component,
                    _extends({ rows: realRows }, otherProps),
                    this.renderTableHeaders()
                );
            }
        }]);

        return Sortable;
    }(_react2.default.Component);

    Sortable.propTypes = propTypes;
    return Sortable;
};