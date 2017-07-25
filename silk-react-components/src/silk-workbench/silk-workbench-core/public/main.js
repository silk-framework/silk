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

/* global dialogPolyfill:true, componentHandler: true */

/**
 * Global JavaScript functions.
 */

/* exported contentWidth
silk-react-components/src/silk-workbench/silk-workbench-rules/public/js/links.js
silk-react-components/src/silk-workbench/silk-workbench-rules/public/js/population.js
*/
var helpWidth = 170;
var contentWidth;
var contentWidthCallback = function() {};
// The currently open dialog
var primary_dialog;
var secondary_dialog;
var dialogs = {};

let dialog;

$(function() {
    // Initialize window
    var id;
    $(window).resize(function() {
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
        primary_dialog
            .querySelector('.close')
            .addEventListener('click', function() {
                primary_dialog.close();
            });
    }
    secondary_dialog = document.querySelector('#secondary_dialog');
    if (secondary_dialog) {
        dialogs.secondary = secondary_dialog;
        if (!secondary_dialog.showModal) {
            dialogPolyfill.registerDialog(secondary_dialog);
        }
        secondary_dialog
            .querySelector('.close')
            .addEventListener('click', function() {
                secondary_dialog.close();
            });
    }
});

/* exported errorHandler
silk-workbench/silk-workbench-rules/app/views/learning/activeLearn.scala.html
 */
var errorHandler = function(request) {
    if (request.responseText) {
        alert(request.responseText);
    } else {
        alert(request.statusText);
    }
};

/* exported showDialog, reloadDialog, closeDialog
Open/Reload Dialog:
silk-react-components/src/silk-workbench/silk-workbench-workspace/public/workspace.js
silk-workbench/silk-workbench-core/app/views/main.scala.html
silk-workbench/silk-workbench-rules/app/views/learning/activeLearn.scala.html
silk-workbench/silk-workbench-rules/app/views/referenceLinks/referenceLinks.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/activities.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/activity/activityControl.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/dataset/datasetDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/removeResourceDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/resourcesDialog.scala.html
Close Dialog:
silk-workbench/silk-workbench-core/app/views/aboutDialog.scala.html
silk-workbench/silk-workbench-core/app/views/widgets/pluginDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/dialogs/deleteRuleDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/dialogs/linkingTaskDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/dialogs/transformationTaskDialog.scala.html
silk-workbench/silk-workbench-rules/app/views/learning/activeLearnDetails.scala.html
silk-workbench/silk-workbench-rules/app/views/learning/resetDialog.scala.html
silk-workbench/silk-workbench-workflow/app/views/workflow/workflowTaskDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/activity/projectActivityConfigDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/activity/taskActivityConfigDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/cloneProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/cloneTaskDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/dataset/datasetDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/executeProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/importLinkSpecDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/importProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/newProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/prefixDialog_improved.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/prefixDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/removeProjectDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/removeResourceDialog.scala.html
silk-workbench/silk-workbench-workspace/app/views/workspace/removeTaskDialog.scala.html
 */

/**
 * Opens a dialog.
 */
function showDialog(path, dialog_key = 'primary', payload = {}) {
    dialog = dialogs[dialog_key];
    $.data(dialog, 'path', path);
    $.get(path, payload, function(data) {
        // inject dialog content into dialog container
        $(dialog).html(data);
        // enable MDL JS for dynamically added components
        componentHandler.upgradeAllRegistered();
    })
        .success(function() {
            dialog.showModal();
        })
        .fail(function(request) {
            alert(request.responseText);
        });
}

/**
 * Reloads a dialog.
 */
function reloadDialog(dialog_key = 'primary') {
    dialog = dialogs[dialog_key];
    var path = $.data(dialog, 'path');
    $.get(path, function(data) {
        $(dialog).html(data);
        componentHandler.upgradeAllRegistered();
    }).fail(function(request) {
        alert(request.responseText);
    });
}

/**
 * Closes current dialog.
 */
function closeDialog(dialog_key = 'primary') {
    dialog = dialogs[dialog_key];
    dialog.close();
}

// TODO Apparently unused?
/* exported showHelp, hideHelp */

/**
 * Shows the help sidebar.
 */
function showHelp() {
    updateHelpWidth(170);
    $('#show-help').hide();
    $('#help').show('slide', {direction: 'right'}, 'slow');
}

/**
 * Hides the help sidebar.
 */
function hideHelp() {
    $('#help').hide('slide', {direction: 'right'}, 'slow', function() {
        updateHelpWidth(16);
        $('#show-help').show();
    });
}

function updateHelpWidth(newWidth) {
    helpWidth = newWidth;
    contentWidth = $(window).width() - helpWidth;
    contentWidthCallback();
}
