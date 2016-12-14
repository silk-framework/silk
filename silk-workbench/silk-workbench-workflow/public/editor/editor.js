function WorkflowEditor() {

  this.styles = {};
  this.styles.connectors = {};
  this.styles.endpoints = {};

  this.styles.connectors.plain = {
    lineWidth: 4,
    strokeStyle: "#61B7CF",
    joinstyle: "round"
  };
  this.styles.connectors.hover = {
    strokeStyle: "#216477"
  }

  this.styles.endpoints.source = {
    anchor: "RightMiddle",
    endpoint: "Dot",
    paintStyle: {
      fillStyle: "#3187CF",
      radius: 4
    },
    connectorStyle: this.styles.connectors.plain,
    connectorHoverStyle: this.styles.connectors.hover,
    connectorOverlays: [ [ "Arrow", { location: 1, width: 15, length: 15 } ] ],
    connector: [ "Flowchart", { stub: 10, cornerRadius: 5 } ],
    isSource: true,
    maxConnections: -1
  }
  this.styles.endpoints.target = {
    anchor: "LeftMiddle",
    endpoint: "Dot",
    paintStyle: {
      fillStyle: "#3187CF",
      radius: 4
    },
    connectorStyle: this.styles.connectors.plain,
    isTarget: true,
    maxConnections: 1
  }
  this.styles.endpoints.dynamic_target = {
    anchor: "LeftMiddle",
    endpoint: "Dot",
    paintStyle: {
      fillStyle: "#3187CF",
      radius: 4
    },
    connectorStyle: this.styles.connectors.plain,
    isTarget: true,
    maxConnections: 1
  }


  this.handler = new DynamicEndpointHandler();
  this.handler.styles = this.styles.endpoints;

  var _this = this;

  // Set jsPlumb default values
  jsPlumb.setContainer("editorContent");

  this.bindEvents = function() {
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
    }.init());

    // Handle dropped operators
    $("#editorContent").droppable({
      drop: function (ev, ui) {

        // Check if we still need to add endpoints to the dropped element
        if(jsPlumb.getEndpoints(ui.helper) === undefined) {
          var id = ui.helper.attr('id');
          // Hide operator in toolbox
          // if($(ui.helper).hasClass('dataset')) {
          //   ui.draggable.hide();
          // }

          // Add operator to editor contents
          $.ui.ddmanager.current.cancelHelperRemoval = true;
          ui.helper.appendTo(this);

          // Make operator draggable
          jsPlumb.draggable(ui.helper);

          // Add endpoints
          jsPlumb.addEndpoint(id, _this.styles.endpoints.source);
          var inputCardinality = ui.helper.data().inputCardinality;
          if (inputCardinality == -1) {
            _this.handler.addDynamicEndpoint(id, "dynamic_target");
            // jsPlumb.addEndpoint(id, _this.styles.endpoints.dynamic_target);
          } else {
            var endpoints = []
            for (index = 0; index < inputCardinality; index++) {
              endpoints.push(jsPlumb.addEndpoint(id, _this.styles.endpoints.target));
            }
            _this.handler.repaintEndpoints(id, endpoints);
          }
        }
      }
    });

    // Delete connections on clicking them
    jsPlumb.bind("click", function(conn, originalEvent) {
      jsPlumb.detach(conn);
    });

  };

  this.deserializeWorkflow = function(xml) {
    // Retrieve the xml root element
    var xmlRoot = xml.children('Workflow');
    // Find the editor contents to put the operators into
    var editorContent = $("#editorContent");
    // Remember generated endpoints
    var sourceEndpoints = {};
    var targetEndpoints = {};

    deserializeWorkflowOperator('Operator', 'operator');
    deserializeWorkflowOperator('Dataset', 'dataset');

    function deserializeWorkflowOperator(elementName, childClass) {
      xmlRoot.find(elementName).each(function() {
        var xml = $(this);
        var taskId = xml.attr('task');
        var opId = xml.attr('id');
        var outputPriority = xml.attr('outputPriority')
        if(opId === undefined) {
          opId = taskId
        }

        var toolbox = $("#toolbox_" + taskId);
        // Don't hide, workflow operators can be used multiple times
        //    toolbox.hide();

        var box = toolbox.children('.' + childClass).clone(false);
        if(outputPriority) {
          box.attr('output_priority', outputPriority);
        }
        box.attr('taskid', taskId);
        box.attr('id', opId)
        box.show();
        box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
        box.appendTo(editorContent);

        // Make operator draggable
        jsPlumb.draggable(box);

        // Add endpoints
        sourceEndpoints[opId] = jsPlumb.addEndpoint(box, _this.styles.endpoints.source);
        var inputCardinality = $(box).data().inputCardinality;
        targetEndpoints[opId] = [];
        if (inputCardinality == -1) {
          targetEndpoints[opId].push(_this.handler.addDynamicEndpoint(box, "dynamic_target"));
          // jsPlumb.addEndpoint(id, _this.styles.endpoints.dynamic_target);
        } else {
          for (index = 0; index < inputCardinality; index++) {
            targetEndpoints[opId].push(jsPlumb.addEndpoint(box, _this.styles.endpoints.target));
          }
          _this.handler.repaintEndpoints(box, targetEndpoints[opId]);
        }
      });
    }

    connectEndpoints('Operator');
    connectEndpoints('Dataset');

    function connectEndpoints(elementName) {
      // Connect endpoints
      // Since operators are connected in both directions we only need to look at one direction, i.e. inputs.
      xmlRoot.find(elementName).each(function() {
        var xml = $(this);

        var taskId = xml.attr('id');
        // Connect inputs
        var inputCardinality = $("#" + taskId).data().inputCardinality;
        $.each(xml.attr('inputs').split(','), function(index, value) {
          if(value != "") {
            jsPlumb.connect({source: sourceEndpoints[value], target: targetEndpoints[taskId][index]});
            if (inputCardinality == -1) {
              // these are dynamic enpoints, so we need to get the last one and push it on the stack of
              // endpoints for this taskId, to make it available in the next iteration of the loop
              var openEndpoint = _this.handler.getOpenDynamicEndpoint(taskId);
              targetEndpoints[taskId].push(openEndpoint);
            }
          }
        });
      });
    }
  };

  this.loadWorkflow = function() {
    $.get(apiUrl, function(data) {
      _this.deserializeWorkflow($(data));
    })
    .fail(function(msg) {
      alert( "Error loading workflow from backend: " + msg);
    })
  };

  this.removeElement = function(elementId) {
    //We need to set a time-out here as a element should not remove its own parent in its event handler
    setTimeout(function() {
      // Remove the elemenet from the workflow
      jsPlumb.removeAllEndpoints(elementId);
      $('#' + elementId).remove();
      // Show the corresponding element in the toolbox again
      $('#toolbox' + elementId.substring(elementId.indexOf("_"))).show();
    }, 100);
  };

  this.bindEvents();

}


$(function () {

  jsPlumb.ready(function() {
    editor = new WorkflowEditor();
    // Load workflow from backend
    editor.loadWorkflow();
  });

});

