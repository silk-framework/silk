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

var aggregatecounter = 0;
var transformcounter = 0;
var comparecounter = 0;
var sourcecounter = 0;
var elementcounter = 0;
var numberElements = 0;

var transformations = new Object();
var comparators = new Object();
var aggregators = new Object();

var sources = new Array();
var targets = new Array();
var boxes = new Array();

var cycleFound = false;

var modificationTimer;
var reverting = false;

var instanceStack = new Array();
var instanceIndex = -1;
var instanceSaved = false;

var confirmOnExit = false;

jsPlumb.Defaults.Container = "droppable";

var valueConnectorStyle = {
  lineWidth:4,
  strokeStyle: "#61B7CF",
  joinstyle: "round"
};

var similarityConnectorStyle = {
  lineWidth:4,
  strokeStyle: "#BF7761",
  joinstyle: "round"
};

var endpointValueSource = {
  anchor: "RightMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#3187CF",
    radius: 4
  },
  connectorStyle: valueConnectorStyle,
  connectorOverlays: [ [ "Arrow", { location: 1, width: 15, length: 15 } ] ],
  connector: [ "Flowchart", { stub: 10, cornerRadius: 5 } ],
  isSource: true,
  scope: "value"
};

var endpointValueTarget = {
  anchor: "LeftMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#3187CF",
    radius: 4
  },
  connectorStyle: valueConnectorStyle,
  isTarget: true,
  scope: "value",
  maxConnections: -1
};

var endpointSimilaritySource = {
  anchor: "RightMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#BF5741",
    radius: 4
  },
  connectorStyle: similarityConnectorStyle,
  connectorOverlays: [ [ "Arrow", { location: 1, width: 15, length: 15 } ] ],
  connector: [ "Flowchart", { stub: 10, cornerRadius: 5 } ],
  isSource: true,
  scope: "similarity"
};

var endpointSimilarityTarget = {
  anchor: "LeftMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#BF5741",
    radius: 4
  },
  connectorStyle: similarityConnectorStyle,
  isTarget: true,
  scope: "similarity",
  maxConnections: -1
};

document.onselectstart = function ()
{
  return false;
};

// Warn the user when he leaves the editor that any unsaved modifications are lost.
window.onbeforeunload = confirmExit;

$(function ()
{
  $("#droppable").droppable({
    drop: function (ev, ui) {
      var draggedId = $(ui.draggable).attr("id");
      var boxid = ui.helper.attr('id');

      // Check if we still need to add endpoints to the dropped element
      if(jsPlumb.getEndpoints(ui.helper) === undefined) {
        $.ui.ddmanager.current.cancelHelperRemoval = true;
        ui.helper.appendTo(this);

        // Set operator name to current id
        $('#' + boxid + " > .content > .label").text(boxid);

        // Make operator draggable
        jsPlumb.draggable($('#' + boxid));

        if (draggedId.search(/aggregator/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointSimilarityTarget);
          jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
        }
        else if (draggedId.search(/comparator/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueTarget);
          jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
        }
        else if (draggedId.search(/transform/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueSource);
          jsPlumb.addEndpoint(boxid, endpointValueTarget);
        }
        else if (draggedId.search(/source/) != -1 || draggedId.search(/target/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueSource);
        }
        else {
          alert("Invalid Element dropped: " + draggedId);
        }

        // fix the position of the new added box
        var offset = $('#' + boxid).offset();
        var scrollleft = $("#droppable").scrollLeft();
        var scrolltop = $("#droppable").scrollTop();
        var top = offset.top-118+scrolltop+scrolltop;
        var left = offset.left-504+scrollleft+scrollleft;
        $('#' + boxid).attr("style", "left: " + left + "px; top: " + top +  "px; position: absolute;");
        jsPlumb.repaint(boxid);
        modifyLinkSpec();
      }
    }
  });

  $('body').attr('onresize', 'updateWindowSize();');
  $('body').attr('onunload', 'jsPlumb.unload();');

  // Update whenever a new connection has been established
  jsPlumb.bind("connection", function(info) {
    modifyLinkSpec();
  });

  // Update whenever a connection has been removed
  jsPlumb.bind("connectionDetached", function(info) {
    modifyLinkSpec();
  });

  // Update whenever a parameter has been changed
  $(document).on('change', "input[type!='text']", function(info) {
    modifyLinkSpec();
  });
  $(document).on('keyup', "input[type='text']", function(info) {
    modifyLinkSpec();
  });

  $("#undo").button({ disabled: true });
  $("#redo").button({ disabled: true });

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

  $(document).on('click', ".label", function() {
    var current_label = $(this).html();
    var input = '<input class="label-change" type="text" value="' + current_label + '" />';
    $(this).html(input).addClass('label-active').removeClass('label');
    $(this).children().focus();
  });

  $(document).on('blur', ".label-change", function() {
    var new_label = $(this).val();
    $(this).parent().html(new_label).addClass('label').removeClass('label-active');
  });

  $(document).on('click', "div.sourcePath > h5 > div[class!='active'], div.targetPath > h5 > div[class!='active']", function() {
    var thisPath = $(this).text();
    if (!thisPath) thisPath = $(this).children().val();
    if (thisPath !== undefined) thisPath = encodeHtml(thisPath);
    $(this).addClass('active');
    $(this).html('<input class="new-path" type="text" value="' + thisPath + '" />');
    $(this).parent().css("height", "19px");
    $(this).children().focus();
  });

  $(document).on('blur',".new-path", function() {
    var newPath = $(this).val();
    if (newPath !== undefined) newPath = encodeHtml(newPath);
    $(this).parent().parent().css("height", "15px");
    $(this).parent().parent().parent().children(".name").html(newPath);
    $(this).parent().removeClass('active').attr('title', newPath).html(newPath);
  });

  $(document).on('mouseover', "#source_restriction, #target_restriction", function() {
    var txt = $(this).text();
    Tip(txt, DELAY, 20);
  });

  $(document).on('mouseout', "#source_restriction, #target_restriction", function() {
    UnTip();
  });

});

function confirmExit() {
  if(confirmOnExit) {
    return "The current linkage rule is invalid. Leaving the editor will revert to the last valid linkage rule.";
  }
}

function generateNewElementId() {
  var nameExists;
  do {
    nameExists = false;
    elementcounter = elementcounter + 1;
    $("div.label:contains('unnamed_" + elementcounter + "')").each(function() {
      if(("unnamed_" + elementcounter).length == $(this).text().length) {
        nameExists = true;
      }
    });
  } while (nameExists || $("#unnamed_"+elementcounter).length > 0);
  return "unnamed_"+elementcounter;
}

function getCurrentElementName(elId) {
  var elName = $("#" + elId + " .content > .label").text();
  if (!elName) elName = $("#" + elId + " .content > .label-active > input.label-change").val();
  return elName;
}

function validateLinkSpec() {
  var errors = new Array();
  var root_elements = new Array();
  var totalNumberElements = 0;
  removeHighlighting();
  if (!reverting) {
    saveInstance();
  }
  reverting = false;

  // if only one element exists
  // Is allowed in transformation rule editor
//  if ($("#droppable > div.dragDiv").length === 1) {
//    var elId = $("#droppable > div.dragDiv").attr('id');
//    errorObj = new Object();
//    errorObj.id = elId;
//    errorObj.message = "Error: Unconnected element '" + getCurrentElementName(elId) + "'.";
//    errors.push(errorObj);
//  }

  $("#droppable > div.dragDiv").each(function() {
    totalNumberElements++;
    var elId = $(this).attr('id');
    var elName = getCurrentElementName(elId);
    if (elName.search(/[^a-zA-Z0-9_-]+/) !== -1) {
      errorObj = new Object;
      errorObj.id = elId;
      errorObj.message = "Error in element with id '"+ elId +"': An identifier may only contain the following characters (a - z, A - Z, 0 - 9, _, -). The following identifier is not valid: '" + elName + "'.";
      errors.push(errorObj);
    }
    // count root elements
    var target = jsPlumb.getConnections({scope: ['value', 'similarity'], source: elId}, true);
    if (target === undefined || target.length == 0) {
      root_elements.push(elId);
    }
  });

  if (errors.length == 0) {

    // multiple root elements
    if (root_elements.length > 1) {
      errorObj = new Object();
      var elements = "";
      for (var i = 0; i<root_elements.length; i++) {
        elements += "'" + getCurrentElementName(root_elements[i]) + "'";
        if (i<root_elements.length-1) {
          elements += ", ";
        } else {
          elements += ".";
        }
        highlightElement(root_elements[i], "Error: Multiple root elements found.");
      }
      errorObj.message = "Error: Multiple root elements found: " + elements;
      errorObj.id = 0;
      errors.push(errorObj);

      // no root elements
    } else if (root_elements.length == 0 && totalNumberElements > 0) {
      errorObj = new Object;
      errorObj.id = 0;
      errorObj.message = "Error: No root element found.";
      errors.push(errorObj);

      // cycles
    } else {
      cycleCheck(root_elements[0]);
      if (cycleFound) {
        errorObj = new Object();
        errorObj.id = 0;
        errorObj.message = "Error: A cycle was found in the linkage rule.";
        errors.push(errorObj);
      }
    }

    // forest found
    if ((numberElements > 1) && (totalNumberElements > numberElements)) {
      errorObj = new Object();
      errorObj.id = 0;
      errorObj.message = "Error: Multiple linkage rules found.";
      errors.push(errorObj);
    }

    $("#droppable > div.dragDiv").removeAttr("visited");
    cycleFound = false;
    numberElements = 0;
  }

  if (errors.length > 0) {
    // display frontend errors
    updateStatus(errors, null, null);
  } else {
    // send to server
    $.ajax({
      type: 'PUT',
      url: apiUrl + '/rule',
      contentType: 'text/xml',
      processData: false,
      data: serializationFunction(),
      dataType: "json",
      success: function(response) {
        updateStatus(response.error, response.warning, response.info);
        updateScore();
      },
      error: function(req) {
        console.log('Error committing rule: ' + req.responseText);
        var response = jQuery.parseJSON(req.responseText);
        updateStatus(response.error, response.warning, response.info);
      }
    });
  }
};

function modifyLinkSpec() {
  console.log("modifyLinkSpec");
  confirmOnExit = true;
  showPendingIcon();
  clearTimeout(modificationTimer);
  modificationTimer = setTimeout(function() { validateLinkSpec(); }, 2000);
}

function updateStatus(errorMessages, warningMessages, infoMessages) {
  $("#info-box").html("");
  if (errorMessages.length > 0) {
    $("#info-box").append(printErrorMessages(errorMessages));
    showInvalidIcon(errorMessages.length);
  } else if (warningMessages.length > 0) {
    confirmOnExit = false;
    $("#info-box").append(printMessages(warningMessages));
    showWarningIcon(warningMessages.length);
  } else {
    confirmOnExit = false;
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

function highlightElement(elId, message) {
  var elementToHighlight;
  $(".label:contains('" + elId + "')").each(function() {
    if (elId.length == $(this).text().length) elementToHighlight = $(this).parent().parent();
  });
  if (!elementToHighlight) elementToHighlight = $("#" + elId);
  elementToHighlight.addClass('highlighted').attr('onmouseover', 'Tip("' + encodeHtml(message) + '")').attr("onmouseout", "UnTip()");
  jsPlumb.repaint(elementToHighlight);
}

function removeHighlighting() {
  $("div .dragDiv").removeClass('highlighted').removeAttr('onmouseover');
  jsPlumb.repaintEverything();
}

function cycleCheck(elId) {
  numberElements++;
  if ($("#"+elId).attr("visited") == "1") {
    cycleFound = true;
  } else {
    $("#"+elId).attr("visited","1");
    var sources = jsPlumb.getConnections({target: elId});
    if (sources[jsPlumb.getDefaultScope()] !== undefined) {
      for (var i = 0; i < sources[jsPlumb.getDefaultScope()].length; i++) {
        var source = sources[jsPlumb.getDefaultScope()][i].sourceId;
        cycleCheck(source);
      }
    }
  }
}

Array.max = function(array) {
  return Math.max.apply(Math, array);
};

function removeElement(elementId) {
  //We need to set a time-out here as a element should not remove its own parent in its event handler
  setTimeout(function() {
    jsPlumb.removeAllEndpoints(elementId);
    $('#' + elementId).remove();
    UnTip();
    modifyLinkSpec();
  }, 100);
}

function updateWindowSize() {
  var window_width =  $(window).width();
  var window_height =  $(window).height();
  if (window_width > 1100) {
    $(".wrapperEditor").width(window_width-10);
    $("#droppable").width(window_width-290);
  }
  if (window_height > 600) {
    $(".droppable_outer, #droppable").height(window_height - 165);
    var scrollboxes = $(".scrollboxes");
    scrollboxes.height((window_height - 165)/scrollboxes.length - 25);
  }
}

function rearrangeBoxes() {
  for (var i = boxes.length - 1; i >= 0; i--) {
    for (var j = 0; j < boxes[i].length; j++) {

      var box = boxes[i][j];
      var box_id = box.attr("id");
      var child_conns = jsPlumb.getConnections({target: box_id});
      var children = child_conns[jsPlumb.getDefaultScope()];
      if (children.length == 1) {
        var child = children[0].source;
        child.css("top",box.css("top"));
      }
      if (children.length > 1) {
        var first_child = children[0].source;
        var last_child = children[children.length-1].source;
        var top_first = parseInt(first_child.css("top"));
        var bottom_last = parseInt(last_child.css("top")) + parseInt(last_child.height());
        var middle = parseInt((top_first+bottom_last)/2);
        box.css("top",middle-parseInt(box.height()*0.5));
      }
    }
  }
  jsPlumb.repaintEverything();
}

function undo() {
  if (instanceIndex > 0) loadInstance(instanceIndex - 1);
}
function redo() {
  if (instanceIndex < instanceStack.length - 1) loadInstance(instanceIndex + 1);
}

function loadInstance(index) {
    //console.log("loadInstance("+index+")");
    reverting = true;
    instanceIndex = index;
    updateRevertButtons();
    var elements = instanceStack[index];
    var endpoint_right;
    var endpoint_left;

    jsPlumb.detachEveryConnection();
    $("#droppable > div.dragDiv").each(function () {
        jsPlumb.removeAllEndpoints($(this).attr('id'));
        $(this).remove();
    });

    for (var i = 0; i<elements.length; i++) {

       endpoint_right = null;
       endpoint_left = null;
       var box = elements[i][0].clone();
       var boxid = box.attr('id');
       var boxclass = box.attr('class');

        $("#droppable").append(box);

        if (boxclass.search(/aggregate/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="compare"], canvas[elType="aggregate"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "aggregate");
        }
        if (boxclass.search(/transform/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "transform");
        }
        if (boxclass.search(/compare/) != -1)
        {
          endpoint_left = jsPlumb.addEndpoint(boxid, jsPlumb.extend({dropOptions:{ accept: 'canvas[elType="transform"], canvas[elType="source"], canvas[elType="target"]', activeClass: 'accepthighlight', hoverClass: 'accepthoverhighlight', over: function(event, ui) { $("body").css('cursor','pointer'); }, out: function(event, ui) { $("body").css('cursor','default'); } }, uuid: boxid}, endpointOptions1));
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions2);
          box.nextUntil("div", ".ui-draggable").attr("elType", "compare");
        }
        if (boxclass.search(/source/) != -1)
        {
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions);
          box.nextUntil("div", ".ui-draggable").attr("elType", "source");
        }
        if (boxclass.search(/target/) != -1)
        {
          endpoint_right = jsPlumb.addEndpoint(boxid, endpointOptions);
          box.nextUntil("div", ".ui-draggable").attr("elType", "target");
        }

        elements[i][2] = endpoint_right;
        jsPlumb.draggable(box);
    }

    for (var j = 0; j<elements.length; j++) {
        var endp_left = elements[j][2];
        var endp_right = jsPlumb.getEndpoint(elements[j][1]);
        if (endp_left && endp_right) {
            jsPlumb.connect({
                sourceEndpoint: endp_left,
                targetEndpoint: endp_right
            });
        }
    }

    modifyLinkSpec();
}

function saveInstance() {
    //console.log("saveInstance");
    var elements = new Array();
    var targets = new Array();
    var i = 0;
    $("#droppable > div.dragDiv").each(function() {
        elements[i] = new Array();
        var box = $(this).clone();
        var id = box.attr('id');
        elements[i][0] = box;
        var conns = jsPlumb.getConnections({source: id});
        targets = conns[jsPlumb.getDefaultScope()];
        if (targets !== undefined && targets.length > 0) {
            var target = targets[0].target;
            elements[i][1] = target.attr('id');
        }
        i++;
    });

    instanceStack[++instanceIndex] = elements;
    instanceStack.splice(instanceIndex + 1);
    updateRevertButtons();
//    if (!instanceSaved) {
//        $("#content").unblock();
//    }
    instanceSaved = true;
}

function updateRevertButtons() {
  if (instanceIndex > 0) {
    $("#undo").button("enable");
  } else {
    $("#undo").button("disable");
  }
  if (instanceIndex  < instanceStack.length - 1) {
    $("#redo").button("enable");
  } else {
    $("#redo").button("disable");
  }
}

function encodeHtml(value) {
  encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}

function getPropertyPaths() {
  $.ajax({
    type: 'get',
    url: editorUrl + '/paths',
    complete: function(response, status) {
      $("#paths").html(response.responseText);
      if(status == "error") {
        setTimeout('getPropertyPaths()', 2000);
      } else {
        updateWindowSize();
      }
    }
  })
}

function reloadPropertyPaths() {
  getPropertyPaths(true);
  var answer = confirm("Reloading the cache may take a long time. Do you want to proceed?");
  if (answer) {
    reloadCache();
  }
}

function reloadCache() {
  $.ajax({
    type: "PUT",
    url: apiUrl + '/tasks/' + projectName + '/' + taskName + '/reloadCache',
    dataType: "xml",
    success: function() { updateScore() }
  });
}

function updateScore() {
  $.ajax({
    type: 'get',
    url: editorUrl + "/score",
    complete: function(response, status) {
      $("#score-widget").html(response.responseText);
      if(status == "error") {
        setTimeout('updateScore()', 2000);
      }
    }
  })
}