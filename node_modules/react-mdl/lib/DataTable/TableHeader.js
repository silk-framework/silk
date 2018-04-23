'use strict';

Object.defineProperty(exports, "__esModule", {
    value: true
});

var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _react = require('react');

var _react2 = _interopRequireDefault(_react);

var _propTypes = require('prop-types');

var _propTypes2 = _interopRequireDefault(_propTypes);

var _classnames = require('classnames');

var _classnames2 = _interopRequireDefault(_classnames);

var _Tooltip = require('../Tooltip');

var _Tooltip2 = _interopRequireDefault(_Tooltip);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

var propTypes = {
    cellFormatter: _propTypes2.default.func, // Used by the Table component to format the cell content for this "column"
    className: _propTypes2.default.string,
    name: _propTypes2.default.string.isRequired,
    numeric: _propTypes2.default.bool,
    onClick: _propTypes2.default.func,
    nosort: _propTypes2.default.bool,
    sortFn: _propTypes2.default.func, // Used by the Sortable component
    tooltip: _propTypes2.default.node
};

var TableHeader = function TableHeader(props) {
    var className = props.className,
        name = props.name,
        numeric = props.numeric,
        onClick = props.onClick,
        nosort = props.nosort,
        tooltip = props.tooltip,
        children = props.children,
        otherProps = _objectWithoutProperties(props, ['className', 'name', 'numeric', 'onClick', 'nosort', 'tooltip', 'children']);

    // remove unwanted props
    // see https://github.com/Hacker0x01/react-datepicker/issues/517#issuecomment-230171426


    delete otherProps.cellFormatter;
    delete otherProps.sortFn;

    var classes = (0, _classnames2.default)({
        'mdl-data-table__cell--non-numeric': !numeric
    }, className);

    var clickFn = !nosort && onClick ? function (e) {
        return onClick(e, name);
    } : null;

    return _react2.default.createElement(
        'th',
        _extends({ className: classes, onClick: clickFn }, otherProps),
        !!tooltip ? _react2.default.createElement(
            _Tooltip2.default,
            { label: tooltip },
            children
        ) : children
    );
};

TableHeader.propTypes = propTypes;

exports.default = TableHeader;