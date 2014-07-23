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
  isSource: true
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
  maxConnections: -1
};

var elementcounter = 0;

$(function () {

  // Set jsPlumb default values
  jsPlumb.setContainer("editorContent");

  // Make operators draggable
  $('.toolboxOperator').draggable({
    helper: function() {
      var box = $(this).children('.operator,.dataset').clone(false);
      // Generate a new id for the operator of the form operator_name
      var boxId = $(this).attr('id');
      box.attr('id', 'operator' + boxId.substring(boxId.indexOf("_")));
      box.show();
      return box;
    }
  });

  $("#editorContent").droppable({
    drop: function (ev, ui) {
      // Check if we still need to add endpoints to the dropped element
      if(jsPlumb.getEndpoints(ui.helper) === undefined) {
        var id = ui.helper.attr('id');

        // Hide operator in toolbox
        ui.draggable.hide();

        // Add operator to editor contents
        $.ui.ddmanager.current.cancelHelperRemoval = true;
        ui.helper.appendTo(this);

        // Make operator draggable
        jsPlumb.draggable(ui.helper);

        // Add endpoints
        jsPlumb.addEndpoint(id, endpointSource);
        jsPlumb.addEndpoint(id, endpointTarget);
      }
    }
  });

  // Delete connections on clicking them
  jsPlumb.bind("click", function(conn, originalEvent) {
    jsPlumb.detach(conn);
  });

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

function removeElement(elementId) {
  //We need to set a time-out here as a element should not remove its own parent in its event handler
  setTimeout(function() {
    // Remove the elemenet from the workflow
    jsPlumb.removeAllEndpoints(elementId);
    $('#' + elementId).remove();
    // Show the corresponding element in the toolbox again
    $('#toolbox' + elementId.substring(elementId.indexOf("_"))).show();
  }, 100);
}