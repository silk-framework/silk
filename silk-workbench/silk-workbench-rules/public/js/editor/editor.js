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

// Set jsPlumb default values
jsPlumb.Defaults.Container = "droppable";
jsPlumb.Defaults.DragOptions = { cursor: 'pointer', zIndex:2000 };

var valueConnectorStyle = {
  lineWidth: 4,
  strokeStyle: "#61B7CF",
  joinstyle: "round"
};

var valueConnectorHoverStyle = {
  strokeStyle: "#216477"
};

var similarityConnectorStyle = {
  lineWidth:4,
  strokeStyle: "#BF7761",
  joinstyle: "round"
};

var similarityConnectorHoverStyle = {
  strokeStyle: "#7F2711"
};

var endpointValueSource = {
  anchor: "RightMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#3187CF",
    radius: 4
  },
  connectorStyle: valueConnectorStyle,
  connectorHoverStyle: valueConnectorHoverStyle,
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
  connectorHoverStyle: similarityConnectorHoverStyle,
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
      var draggedClass = $(ui.draggable).attr("class");
      var boxid = ui.helper.attr('id');

      // Check if we still need to add endpoints to the dropped element
      if(jsPlumb.getEndpoints(ui.helper) === undefined) {
        $.ui.ddmanager.current.cancelHelperRemoval = true;
        ui.helper.appendTo(this);

        // Set operator name to current id
        $('#' + boxid + " .handler label").text(boxid);

        // Make operator draggable
        jsPlumb.draggable($('#' + boxid));

        if (draggedClass.search(/aggregator/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointSimilarityTarget);
          jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
        }
        else if (draggedClass.search(/comparator/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueTarget);
          jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
        }
        else if (draggedClass.search(/transform/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueSource);
          jsPlumb.addEndpoint(boxid, endpointValueTarget);
        }
        else if (draggedClass.search(/source/) != -1 || draggedClass.search(/target/) != -1) {
          jsPlumb.addEndpoint(boxid, endpointValueSource);
        }
        else {
          alert("Invalid Element dropped: " + draggedClass);
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

  if (inEditorEnv) {
    $('body').attr('onresize', 'updateWindowSize();');
  }
  $('body').attr('onunload', 'jsPlumb.unload();');  // TODO: What is this supposed to do? "jsPlumb.unload is not a function"

  // Delete connections on clicking them
  jsPlumb.bind("click", function(conn, originalEvent) {
    jsPlumb.detach(conn);
  });

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
  $(document).on('keyup', "input[type='text'].param_value", function(info) {
    modifyLinkSpec();
  });

  $("#undo").attr("disabled", true);
  $("#redo").attr("disabled", true);

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

  if (inEditorEnv) {
    updateWindowSize();
    updateScore();
  }
});

function confirmExit() {
  if(confirmOnExit) {
    return "The current linkage rule is invalid. Leaving the editor will revert to the last valid linkage rule.";
  }
}

function generateNewElementId(currentId) {
  var nameExists;
  var counter = 0;
  do {
    nameExists = false;
    counter = counter + 1;
    id = "#" + currentId + counter;
    alternativeId = "#operator_" + currentId + counter;
    if($(id).length > 0 || $(alternativeId).length > 0) {
      nameExists = true;
    }
  } while (nameExists);
  return currentId + counter;
}

function getCurrentElementName(elId) {
  var elName = $("#" + elId + " .handler label").text();
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
      errorObj.type = "Error";
      errorObj.id = elName;
      errorObj.message = "Error in element with id '"+ elName +"': An identifier may only contain the following characters (a - z, A - Z, 0 - 9, _, -). The following identifier is not valid: '" + elName + "'.";
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
      errorObj.type = "Error";
      var elements = "";
      for (var i = 0; i<root_elements.length; i++) {
        var currentElementName = getCurrentElementName(root_elements[i]);
        elements += "'" + currentElementName + "'";
        if (i<root_elements.length-1) {
          elements += ", ";
        } else {
          elements += ".";
        }
        highlightElement(currentElementName, "Error: Multiple root elements found.");
      }
      errorObj.message = "Error: Multiple root elements found: " + elements;
      errors.push(errorObj);

      // no root elements
    } else if (root_elements.length == 0 && totalNumberElements > 0) {
      errorObj = new Object;
      errorObj.type = "Error";
      errorObj.message = "Error: No root element found.";
      errors.push(errorObj);

      // cycles
    } else {
      cycleCheck(root_elements[0]);
      if (cycleFound) {
        errorObj = new Object();
        errorObj.type = "Error";
        errorObj.message = "Error: A cycle was found in the linkage rule.";
        errors.push(errorObj);
      }
    }

    // forest found
    if ((numberElements > 1) && (totalNumberElements > numberElements)) {
      errorObj = new Object();
      errorObj.type = "Error";
      errorObj.message = "Error: Multiple linkage rules found.";
      errors.push(errorObj);
    }

    $("#droppable > div.dragDiv").removeAttr("visited");
    cycleFound = false;
    numberElements = 0;
  }

  if (errors.length > 0) {
    // display frontend errors
    updateEditorStatus(errors);
  } else {
    // send to server
    $.ajax({
      type: 'PUT',
      url: apiUrl + '/rule' + ruleIndex,
      contentType: 'text/xml',
      processData: false,
      data: serializationFunction(),
      dataType: "json",
      success: function(response) {
        updateEditorStatus(response.issues);
        updateScore();
        confirmOnExit = false;
      },
      error: function(req) {
        console.log('Error committing rule: ' + req.responseText);
        updateEditorStatus(req.responseJSON.issues);
      }
    });
  }
};

function modifyLinkSpec() {
  confirmOnExit = true;
  showPendingIcon();
  clearTimeout(modificationTimer);
  modificationTimer = setTimeout(function() { validateLinkSpec(); }, 2000);
}

function updateEditorStatus(messages) {
  // Update status icon
  updateStatus(messages);
  // Highlight elements
  highlightElements(messages);
}

function highlightElements(messages) {
  var c = 1;
  for (var i = 0; i<messages.length; i++) {
    if (messages[i].id) highlightElement(messages[i].id, encodeHtml(messages[i].message));
    c++;
  }
}

function highlightElement(elId, message) {
  $(".handler label").each(function() {
    if ($(this).text() == elId) {
      var elementToHighlight = $(this).parent().parent();
      elementToHighlight.addClass('highlighted').attr('onmouseover', 'Tip("' + encodeHtml(message) + '")').attr("onmouseout", "UnTip()");
      jsPlumb.repaint(elementToHighlight);
    }
  });
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
  var header_height = $("#app_header").height();
  var window_width =  $(window).width();
  var window_height =  $(window).height();
  var content_padding = 35;
  if (window_width > 1100) {
    $(".wrapperEditor").width(window_width-10);
    $("#droppable").width(window_width-295);
  }
  if (window_height > 600) {
    // resize height of drawing canvas
    var height = window_height - header_height - content_padding;
    $(".droppable_outer, #droppable").height(height);
    $(".draggables").height(height);

    // resize palette blocks
    var draggables_padding_height = 10;
    var draggables_border_height = 2;
    var palette_header_height = $("#palette-header").outerHeight();
    var height_diff = palette_header_height + draggables_padding_height + draggables_border_height;
    var palette_blocks = $(".palette-block");
    var palette_block_margin = parseInt(palette_blocks.css('margin-top'));
    palette_block_height = ((height - height_diff)/palette_blocks.length) - palette_block_margin;
    palette_blocks.height(palette_block_height);

    // resize scroll-boxes
    var block_header_height = 34;
    var scrollbox_height = palette_block_height - block_header_height;
    var scrollboxes_grouped = $('#operators-grouped .scrollboxes');
    scrollboxes_grouped.height(scrollbox_height);
    var scrollboxes_search = $('#operators-search-result .scrollboxes');
    scrollboxes_search.height($(".draggables").height() - height_diff);
  }
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
  var endpoints = [];

  jsPlumb.detachEveryConnection();
  $("#droppable").find("> div.dragDiv").each(function () {
    jsPlumb.removeAllEndpoints($(this).attr('id'));
    $(this).remove();
  });

  for (var i = 0; i<elements.length; i++) {
    var endpoint_right = null;
    var endpoint_left = null;
    var box = elements[i][0].clone();
    var boxid = box.attr('id');
    var boxclass = box.attr('class');

    $("#droppable").append(box);

    if (boxclass.search(/aggregate/) != -1) {
      endpoint_left = jsPlumb.addEndpoint(boxid, endpointSimilarityTarget);
      endpoint_right = jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
    }
    else if (boxclass.search(/compare/) != -1) {
      endpoint_left = jsPlumb.addEndpoint(boxid, endpointValueTarget);
      endpoint_right = jsPlumb.addEndpoint(boxid, endpointSimilaritySource);
    }
    else if (boxclass.search(/transform/) != -1) {
      endpoint_left = jsPlumb.addEndpoint(boxid, endpointValueTarget);
      endpoint_right = jsPlumb.addEndpoint(boxid, endpointValueSource);
    }
    else if (boxclass.search(/source/) != -1 || boxclass.search(/target/) != -1) {
      endpoint_right = jsPlumb.addEndpoint(boxid, endpointValueSource);
    }
    else {
      alert("Invalid Element dropped: " + boxclass);
    }

    endpoints[boxid] = endpoint_left;
    elements[i][2] = endpoint_right;
    jsPlumb.draggable(box);
  }

  for(var j = 0; j < elements.length; j++) {
    var endp_left = elements[j][2];
    var endp_right = endpoints[elements[j][1]];
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
    $("#droppable").find("> div.dragDiv").each(function() {
        elements[i] = new Array();
        var box = $(this).clone();
        var id = box.attr('id');
        elements[i][0] = box;
        var conns = jsPlumb.getConnections({scope: ['value', 'similarity'], source: id}, true);
        targets = conns;
        if (targets !== undefined && targets.length > 0) {
            var target = targets[0].target;
            elements[i][1] = target.id;
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
    $("#undo").attr("disabled", false);
  } else {
    $("#undo").attr("disabled", true);
  }
  if (instanceIndex  < instanceStack.length - 1) {
    $("#redo").attr("disabled", false);
  } else {
    $("#redo").attr("disabled", true);
  }
}

function encodeHtml(value) {
  var encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}

/**
 * Replaces targetElement with the list of paths.
 * @targetElement jQuery selector to define the target element
 * @groupPath boolean - true if we want the grouped list, false if we want an ungrouped list (for the keyword search results)
 */
function getPropertyPaths(targetElement, groupPaths) {
  $.ajax({
    type: 'get',
    url: editorUrl + '/widgets/paths',
    data: { groupPaths: groupPaths },
    complete: function(response, status) {
      $(targetElement).html(response.responseText);
      if(status == "error") {
        setTimeout('getPropertyPaths(' + targetElement + ')', 2000);
      } else {
        updateWindowSize();
      }
    }
  })
}

function reloadPropertyPaths() {
  var answer = confirm("Reloading the cache may take a long time. Do you want to proceed?");
  if (answer) {
    reloadCache();
  }
}

function reloadCache() {
  $.ajax({
    type: "POST",
    url: apiUrl + '/reloadCache',
    dataType: "xml",
    success: function() {
      getPropertyPaths('#paths');
      updateScore();
    }
  });
}

function updateScore() {
  $.ajax({
    type: 'get',
    url: editorUrl + "/widgets/score",
    complete: function(response, status) {
      $("#score-widget").html(response.responseText);
      if(status == "error") {
        setTimeout('updateScore()', 2000);
      }
    }
  })
}