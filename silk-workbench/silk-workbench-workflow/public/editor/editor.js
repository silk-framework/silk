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
  maxConnections: -1
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

$(function () {

  var callMe = function() {

    // Set jsPlumb default values
    jsPlumb.setContainer("editorContent");

    // Load workflow from backend
    loadWorkflow();

    // Make operators draggable
    $('.toolboxOperator').draggable({
        init: function() {

          this.helper = function() {
            var counter = 1;
            var box = $(this).children('.operator,.dataset').clone(false);
            // Generate a new id for the operator of the form operator_name
            var boxId = $(this).attr('id');
            var taskId = boxId.substring(boxId.indexOf("_") + 1)
            var suffix = '';
            // Count up if element id already exists
            if(counter > 1) {
              operatorId = taskId + counter;
            } else {
              operatorId = taskId;
            }
            while($('#' + operatorId).length > 0) {
              // Count up because an operator with this id already exists
              counter = counter + 1;
              operatorId = taskId + counter;
            }

            if(counter > 1) {
              suffix = '' + counter;
            }
            box.attr('taskid', taskId)
            box.attr('id', taskId + suffix);
            box.show();
            return box;
          }
          return this;
        }
      }.init()
    );

    // Handle dropped operators
    $("#editorContent").droppable({
      drop: function (ev, ui) {
        // Check if we still need to add endpoints to the dropped element
        if(jsPlumb.getEndpoints(ui.helper) === undefined) {
          var id = ui.helper.attr('id');
          // Hide operator in toolbox
  //        if($(ui.helper).hasClass('dataset')) {
  //          ui.draggable.hide();
  //        }

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
  };

  jsPlumb.ready(function() {
    callMe();
  });
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
