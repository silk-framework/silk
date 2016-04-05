
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
  // Find the editor contents to put the operators into
  var editorContent = $("#editorContent");
  // Remember generated endpoints
  var sourceEndpoints = {};
  var targetEndpoints = {};

  // Delete current operators
  jsPlumb.reset();
  editorContent.empty();

  // Deserialize all datasets
  xmlRoot.find('Dataset').each(function() {
    var xml = $(this);
    var taskId = xml.attr('task');
    var opId = xml.attr('id');
    if(opId === undefined) {
      opId = 'dataset_' + taskId
    }

    var toolbox = $("#toolbox_" + taskId);
    toolbox.hide();

    var box = toolbox.children('.dataset').clone(false);
    box.attr('taskId', 'dataset_' + taskId);
    box.attr('id', opId)
    box.show();
    box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    sourceEndpoints[opId] = jsPlumb.addEndpoint(box, endpointSource);
    targetEndpoints[opId] = jsPlumb.addEndpoint(box, endpointTarget);
  });

  // Deserialize all operators
  xmlRoot.find('Operator').each(function() {
    var xml = $(this);
    var taskId = xml.attr('task');
    var opId = xml.attr('id');
    if(opId === undefined) {
      opId = 'operator_' + taskId
    }

    var toolbox = $("#toolbox_" + taskId);
    // Don't hide, operators can be used multiple times
//    toolbox.hide();

    var box = toolbox.children('.operator').clone(false);
    box.attr('taskid', 'operator_' + taskId);
    console.log('opId ' + opId)
    box.attr('id', opId)
    box.show();
    box.css({top: xml.attr('posY') + 'px', left: xml.attr('posX') + 'px', position: 'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    console.log('Blah: ' + box);
    sourceEndpoints[opId] = jsPlumb.addEndpoint(box, endpointSource);
    targetEndpoints[opId] = jsPlumb.addEndpoint(box, endpointTarget);
  });

  // Connect endpoints
  xmlRoot.find('Operator').each(function() {
    var xml = $(this);
    console.log(xml.attr('id') + '   ' + xml.attr('task'));

    var taskId = xml.attr('id');
    // Connect inputs
    $.each(xml.attr('inputs').split(','), function() {
      if(this != "") {
        jsPlumb.connect({source: sourceEndpoints[this], target: targetEndpoints[taskId]});
      }
    });
    // Connect outputs
    $.each(xml.attr('outputs').split(','), function() {
      if(this != "") {
        jsPlumb.connect({source: sourceEndpoints[taskId], target: targetEndpoints[this]});
      }
    });
  });
}