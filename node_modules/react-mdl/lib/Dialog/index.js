'use strict';

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DialogActions = exports.DialogContent = exports.DialogTitle = exports.Dialog = undefined;

var _Dialog = require('./Dialog');

Object.defineProperty(exports, 'Dialog', {
  enumerable: true,
  get: function get() {
    return _interopRequireDefault(_Dialog).default;
  }
});

var _DialogTitle = require('./DialogTitle');

Object.defineProperty(exports, 'DialogTitle', {
  enumerable: true,
  get: function get() {
    return _interopRequireDefault(_DialogTitle).default;
  }
});

var _DialogActions = require('./DialogActions');

Object.defineProperty(exports, 'DialogActions', {
  enumerable: true,
  get: function get() {
    return _interopRequireDefault(_DialogActions).default;
  }
});

var _basicClassCreator = require('../utils/basicClassCreator');

var _basicClassCreator2 = _interopRequireDefault(_basicClassCreator);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

var DialogContent = exports.DialogContent = (0, _basicClassCreator2.default)('DialogContent', 'mdl-dialog__content');