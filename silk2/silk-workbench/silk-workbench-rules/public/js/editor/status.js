/**
 * The Javascript for the status.scala.html template.
 */

$(function () {
  $("#exclamation, #warning").mouseover(function() {
    $(this).attr("style", "cursor:pointer;");
  });

  $("#exclamation, #warning").click(function() {
    if ($("#info-box").is(':visible')) {
      $("#info-box").slideUp(200);
    } else {
      $("#info-box").css("left", $(window).width()-294+"px");
      $("#info-box").slideDown(200);
    }
  });
});

/**
 * Displays messages.
 * Each parameter contains an array of objects consisting of the following properties:
 * id (optional): The id of the corresponding rule element
 * message: The message to be displayed
 */
function updateStatus(errorMessages, warningMessages, infoMessages) {
  $("#info-box").html("");
  if (errorMessages != null && errorMessages.length > 0) {
    $("#info-box").append(printErrorMessages(errorMessages));
    showInvalidIcon(errorMessages.length);
  } else if (warningMessages != null && warningMessages.length > 0) {
    $("#info-box").append(printMessages(warningMessages));
    showWarningIcon(warningMessages.length);
  } else {
    $("#info-box").slideUp(200);
    showValidIcon();
  }

  if (infoMessages != null && infoMessages.length > 0) {
    $("#info > .precision").html(infoMessages[0]).css("display", "inline");
    if (infoMessages[1] !== undefined) {
      $("#info > .recall").html(infoMessages[1]).css("display", "inline");
    } else {
      $("#info > .recall").css("display", "none");
    }
    if (infoMessages[2] !== undefined) {
      $("#info > .measure").html(infoMessages[2]).css("display", "inline");
    } else {
      $("#info > .measure").css("display", "none");
    }
    $("#info").css("display", "block");
  }
}

function showValidIcon() {
  $("#exclamation, #warning, #pending").css("display", "none");
  $("#tick").css("display", "block");
}

function showInvalidIcon(numberMessages) {
  $("#exclamation > .number-messages").html(numberMessages);
  $("#tick, #warning, #pending").css("display", "none");
  $("#exclamation").css("display", "block");
}

function showWarningIcon(numberMessages) {
  $("#warning > .number-messages").html(numberMessages);
  $("#tick, #exclamation, #pending").css("display", "none");
  $("#warning").css("display", "block");
}

function showPendingIcon() {
  $("#exclamation, #warning, #tick").css("display", "none");
  $("#pending").css("display", "block");
}

function printMessages(array) {
  var result = "";
  var c = 1;
  for (var i = 0; i<array.length; i++) {
    result = result + '<div class="msg">' + c + '. ' + encodeHtml(array[i]) + '</div>';
    c++;
  }
  return result;
}

function printErrorMessages(array) {
  var result = "";
  var c = 1;
  for (var i = 0; i<array.length; i++) {
    result = result + '<div class="msg">' + c + '. ' + encodeHtml(array[i].message) + '</div>';
    if (array[i].id) highlightElement(array[i].id, encodeHtml(array[i].message));
    c++;
  }
  return result;
}

function encodeHtml(value) {
  var encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}
