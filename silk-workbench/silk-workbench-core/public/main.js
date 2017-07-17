'use strict';

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Global JavaScript functions.
 */

var helpWidth = 170;
var contentWidth;
var contentWidthCallback = function contentWidthCallback() {};
// The currently open dialog
var primary_dialog;
var secondary_dialog;
var dialogs = {};

$(function () {
    // Initialize window
    var id;
    $(window).resize(function () {
        clearTimeout(id);
        contentWidth = $(window).width() - helpWidth;
        id = setTimeout(contentWidthCallback, 100);
    });
    contentWidth = $(window).width() - 190;
    contentWidthCallback();

    // Initialize dialog
    primary_dialog = document.querySelector('#primary_dialog');
    if (primary_dialog) {
        dialogs.primary = primary_dialog;
        if (!primary_dialog.showModal) {
            dialogPolyfill.registerDialog(primary_dialog);
        }
        primary_dialog.querySelector('.close').addEventListener('click', function () {
            primary_dialog.close();
        });
    }
    secondary_dialog = document.querySelector('#secondary_dialog');
    if (secondary_dialog) {
        dialogs.secondary = secondary_dialog;
        if (!secondary_dialog.showModal) {
            dialogPolyfill.registerDialog(secondary_dialog);
        }
        secondary_dialog.querySelector('.close').addEventListener('click', function () {
            secondary_dialog.close();
        });
    }
});

var errorHandler = function errorHandler(request) {
    if (request.responseText) {
        alert(request.responseText);
    } else {
        alert(request.statusText);
    }
};

/**
 * Opens a dialog.
 */
function showDialog(path) {
    var dialog_key = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : 'primary';
    var payload = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : {};

    dialog = dialogs[dialog_key];
    $.data(dialog, 'path', path);
    $.get(path, payload, function (data) {
        // inject dialog content into dialog container
        $(dialog).html(data);
        // enable MDL JS for dynamically added components
        componentHandler.upgradeAllRegistered();
    }).success(function () {
        dialog.showModal();
    }).fail(function (request) {
        alert(request.responseText);
    });
}

/**
 * Reloads a dialog.
 */
function reloadDialog() {
    var dialog_key = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : 'primary';

    dialog = dialogs[dialog_key];
    var path = $.data(dialog, 'path');
    $.get(path, function (data) {
        $(dialog).html(data);
        componentHandler.upgradeAllRegistered();
    }).fail(function (request) {
        alert(request.responseText);
    });
}

/**
 * Closes current dialog.
 */
function closeDialog() {
    var dialog_key = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : 'primary';

    dialog = dialogs[dialog_key];
    dialog.close();
}

/**
 * Shows the help sidebar.
 */
function showHelp() {
    updateHelpWidth(170);
    $('#show-help').hide();
    $('#help').show('slide', { direction: 'right' }, 'slow');
}

/**
 * Hides the help sidebar.
 */
function hideHelp() {
    $('#help').hide('slide', { direction: 'right' }, 'slow', function () {
        updateHelpWidth(16);
        $('#show-help').show();
    });
}

function updateHelpWidth(newWidth) {
    helpWidth = newWidth;
    contentWidth = $(window).width() - helpWidth;
    contentWidthCallback();
}
