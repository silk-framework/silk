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
var contentWidthCallback = function() { };
// The currently open dialog
var dialog;
// The path of the current dialog, e.g., /workspace/mydialog
var dialogPath;

$(function() {
  // Style all buttons with jQuery UI
  $("button[type!='radio'], input:submit, a.button").button();

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
  dialog = $('.dialog').dialog({
    autoOpen: false,
    modal: true
  });
});

var errorHandler = function(request) {
  if(request.responseText) {
    alert(request.responseText);
  } else {
    alert(request.statusText)
  }
};

/**
 * Opens a dialog.
 */
function showDialog(path) {
  dialogPath = path;
  $.get(path, function(data) {
    dialog.html(data);
  }).success(function() { dialog.dialog('open'); } )
    .fail(function(request) { alert(request.responseText);  })
}

/**
 * Reloads the current dialog.
 */
function reloadDialog() {
  $.get(dialogPath, function(data) {
    dialog.html(data);
  }).fail(function(request) { alert(request.responseText);  })
}

/**
 * Closes current dialog.
 */
function closeDialog() {
  dialog.dialog('close');
}

/**
 * Shows the help sidebar.
 */
function showHelp() {
  updateHelpWidth(170);
  $('#show-help').hide(); $('#help').show('slide', {direction:'right'}, 'slow');
}

/**
 * Hides the help sidebar.
 */
function hideHelp() {
  $('#help').hide('slide', {direction:'right'}, 'slow', function() { updateHelpWidth(16); $('#show-help').show(); });
}

function updateHelpWidth(newWidth) {
  helpWidth = newWidth;
  contentWidth = $(window).width() - helpWidth;
  contentWidthCallback();
}
