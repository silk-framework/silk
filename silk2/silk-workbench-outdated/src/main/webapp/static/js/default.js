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

$(function() {
  $("button[type!='radio'], input:submit, input:checkbox, a#button").button();

  var id;
  $(window).resize(function() {
    clearTimeout(id);
    contentWidth = $(window).width() - helpWidth;
    id = setTimeout(contentWidthCallback, 100);
  });
  contentWidth = $(window).width() - 190;
  contentWidthCallback();
});

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