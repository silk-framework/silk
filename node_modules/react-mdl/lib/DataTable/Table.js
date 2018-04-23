'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});
exports.UndecoratedTable = undefined;

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _clamp = require('clamp');

var _clamp2 = _interopRequireDefault(_clamp);

var _shadows = require('../utils/shadows');

var _shadows2 = _interopRequireDefault(_shadows);

var _TableHeader = require('./TableHeader');

var _TableHeader2 = _interopRequireDefault(_TableHeader);

var _Selectable = require('./Selectable');

var _Selectable2 = _interopRequireDefault(_Selectable);

var _Sortable = require('./Sortable');

var _Sortable2 = _interopRequireDefault(_Sortable);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

var propTypes = {
    className: _propTypes2.default.string,
    columns: function columns(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use the component `TableHeader` instead.');
    },
    data: function data(props, propName, componentName) {
        return props[propName] && new Error(componentName + ': `' + propName + '` is deprecated, please use `rows` instead. `' + propName + '` will be removed in the next major release.');
    },
    rowKeyColumn: _propTypes2.default.string,
    rows: _propTypes2.default.arrayOf(_propTypes2.default.object).isRequired,
    shadow: _propTypes2.default.number
};

var Table = function (_React$Component) {
    _inherits(Table, _React$Component);

    function Table() {
        _classCallCheck(this, Table);

        return _possibleConstructorReturn(this, (Table.__proto__ || Object.getPrototypeOf(Table)).apply(this, arguments));
    }

    _createClass(Table, [{
        key: 'renderCell',
        value: function renderCell(column, row, idx) {
            var className = !column.numeric ? 'mdl-data-table__cell--non-numeric' : '';
            return _react2.default.createElement(
                'td',
                { key: column.name, className: className },
                column.cellFormatter ? column.cellFormatter(row[column.name], row, idx) : row[column.name]
            );
        }
    }, {
        key: 'render',
        value: function render() {
            var _this2 = this;

            var _props = this.props,
                className = _props.className,
                columns = _props.columns,
                shadow = _props.shadow,
                children = _props.children,
                rowKeyColumn = _props.rowKeyColumn,
                rows = _props.rows,
                data = _props.data,
                otherProps = _objectWithoutProperties(_props, ['className', 'columns', 'shadow', 'children', 'rowKeyColumn', 'rows', 'data']);

            var realRows = rows || data;

            var hasShadow = typeof shadow !== 'undefined';
            var shadowLevel = (0, _clamp2.default)(shadow || 0, 0, _shadows2.default.length - 1);

            var classes = (0, _classnames2.default)('mdl-data-table', _defineProperty({}, _shadows2.default[shadowLevel], hasShadow), className);

            var columnChildren = !!children ? _react2.default.Children.toArray(children).filter(Boolean) : columns.map(function (column) {
                return _react2.default.createElement(
                    _TableHeader2.default,
                    {
                        key: column.name,
                        className: column.className,
                        name: column.name,
                        numeric: column.numeric,
                        tooltip: column.tooltip
                    },
                    column.label
                );
            });
            return _react2.default.createElement(
                'table',
                _extends({ className: classes }, otherProps),
                _react2.default.createElement(
                    'thead',
                    null,
                    _react2.default.createElement(
                        'tr',
                        null,
                        columnChildren
                    )
                ),
                _react2.default.createElement(
                    'tbody',
                    null,
                    realRows.map(function (row, idx) {
                        var _ref = row.mdlRowProps || {},
                            mdlRowPropsClassName = _ref.className,
                            remainingMdlRowProps = _objectWithoutProperties(_ref, ['className']);

                        return _react2.default.createElement(
                            'tr',
                            _extends({
                                key: row[rowKeyColumn] || row.key || idx,
                                className: (0, _classnames2.default)(row.className, mdlRowPropsClassName)
                            }, remainingMdlRowProps),
                            columnChildren.map(function (child) {
                                return _this2.renderCell(child.props, row, idx);
                            })
                        );
                    })
                )
            );
        }
    }]);

    return Table;
}(_react2.default.Component);

Table.propTypes = propTypes;

exports.default = (0, _Sortable2.default)((0, _Selectable2.default)(Table));
var UndecoratedTable = exports.UndecoratedTable = Table;