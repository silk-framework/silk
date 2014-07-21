// Set jsPlumb default values
//jsPlumb.Defaults.Container = "droppable";
jsPlumb.Defaults.DragOptions = { cursor: 'pointer', zIndex:2000 };

var connectorStyle = {
  lineWidth: 4,
  strokeStyle: "#61B7CF",
  joinstyle: "round"
};

var connectorHoverStyle = {
  strokeStyle: "#216477"
};

var endpointSource = {
  anchor: "RightMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#3187CF",
    radius: 4
  },
  connectorStyle: connectorStyle,
  connectorHoverStyle: connectorHoverStyle,
  connectorOverlays: [ [ "Arrow", { location: 1, width: 15, length: 15 } ] ],
  connector: [ "Flowchart", { stub: 10, cornerRadius: 5 } ],
  isSource: true,
  scope: "value"
};

var endpointTarget = {
  anchor: "LeftMiddle",
  endpoint: "Dot",
  paintStyle: {
    fillStyle: "#3187CF",
    radius: 4
  },
  connectorStyle: connectorStyle,
  isTarget: true,
  scope: "value",
  maxConnections: -1
};

var elementcounter = 0;

$(function () {

  // Make operators draggable
  $('.toolboxOperator').draggable({
    helper: function() {
      var box = $(this).children('.operator').clone(false);
      box.attr("id", generateNewElementId());
      box.show();
      return box;
    }
  });

  $("#editorContent").droppable({
    drop: function (ev, ui) {
      var id = ui.helper.attr('id');

      // Add operator to editor contents
      $.ui.ddmanager.current.cancelHelperRemoval = true;
      ui.helper.appendTo(this);

      // Make operator draggable
      jsPlumb.draggable(ui.helper);

      // Add endpoints
      jsPlumb.addEndpoint(id, endpointSource);
      jsPlumb.addEndpoint(id, endpointTarget);
    }
  });

//  // Delete connections on clicking them
//  jsPlumb.bind("click", function(conn, originalEvent) {
//    jsPlumb.detach(conn);
//  });

//  // Update whenever a new connection has been established
//  jsPlumb.bind("connection", function(info) {
//    modifyLinkSpec();
//  });
//
//  // Update whenever a connection has been removed
//  jsPlumb.bind("connectionDetached", function(info) {
//    modifyLinkSpec();
//  });
//
//  // Update whenever a parameter has been changed
//  $(document).on('change', "input[type!='text']", function(info) {
//    modifyLinkSpec();
//  });
//  $(document).on('keyup', "input[type='text']", function(info) {
//    modifyLinkSpec();
//  });
});

function generateNewElementId() {
  elementcounter += 1;
  return "operator_" + elementcounter;
}

function removeElement(elementId) {
  //We need to set a time-out here as a element should not remove its own parent in its event handler
  setTimeout(function() {
    jsPlumb.removeAllEndpoints(elementId);
    $('#' + elementId).remove();
  }, 100);
}