
// Loads workflow from backend
function loadWorkflow() {
  $.get(apiUrl, function(data) {
    deserializeWorkflow($(data));
  })
  .fail(function(msg) {
    alert( "Error loading workflow from backend: " + msg);
  })
}

function deserializeWorkflow(xml) {
  // Retrieve the xml root element
  var xmlRoot = xml.children('Workflow');
  var editorContent = $("#editorContent");
  var sourceEndpoints = {};
  var targetEndpoints = {};

  // Delete current operators
  jsPlumb.reset();
  editorContent.empty();

  xmlRoot.find('Dataset').each(function() {
    var xml = $(this);
    var taskId = xml.attr('task');

    var toolbox = $("#toolbox_" + taskId);
    toolbox.hide();

    var box = toolbox.children('.dataset').clone(false);
    box.attr('id', 'dataset_' + taskId);
    box.show();
    box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    sourceEndpoints[taskId] = jsPlumb.addEndpoint(box, endpointSource);
    targetEndpoints[taskId] = jsPlumb.addEndpoint(box, endpointTarget);
  });

  xmlRoot.find('Operator').each(function() {
    var xml = $(this);
    var taskId = xml.attr('task');

    var toolbox = $("#toolbox_" + taskId);
    toolbox.hide();

    var box = toolbox.children('.operator').clone(false);
    box.attr('id', 'operator_' + taskId);
    box.show();
    box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    sourceEndpoints[taskId] = jsPlumb.addEndpoint(box, endpointSource);
    targetEndpoints[taskId] = jsPlumb.addEndpoint(box, endpointTarget);

    // Connect endpoints
    $.each(xml.attr('inputs').split(','), function() {
      if(this != "") {
        console.log("Adding: " + this + " | " + taskId);
        jsPlumb.connect({source: sourceEndpoints[this], target: targetEndpoints[taskId]});
      }
    });
//    $.each(xml.attr('outputs').split(','), function() {
//      if(this != "") {
//        jsPlumb.connect({source: sourceEndpoints[taskId], target: targetEndpoints[this]});
//      }
//    });
  });
}