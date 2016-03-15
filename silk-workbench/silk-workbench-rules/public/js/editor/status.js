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
function updateStatus(messages) {
  $("#info-box").html("");
  $("#info-box").append(printMessages(messages));

  var errorCount = messages.filter(function(msg){return msg.type == "Error"}).length;
  var warningCount = messages.filter(function(msg){return msg.type == "Warning"}).length;

  if(errorCount > 0) {
    showInvalidIcon(errorCount);
  } else if(warningCount > 0) {
    showWarningIcon(warningCount);
  } else {
    showValidIcon();
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
