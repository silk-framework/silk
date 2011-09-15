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