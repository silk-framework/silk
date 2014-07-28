
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
  var root = xml.children('Workflow');
  var editorContent = $("#editorContent");

  root.find('Dataset').each(function() {
    var toolbox = $("#toolbox_" + $(this).attr('task'));
    toolbox.hide();

    var box = toolbox.children('.dataset').clone(false);
    box.show();
    box.css({top: $(this).attr('posY'), left: $(this).attr('posX'), position:'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    jsPlumb.addEndpoint(box, endpointSource);
    jsPlumb.addEndpoint(box, endpointTarget);
  });

  root.find('Operator').each(function() {
    var toolbox = $("#toolbox_" + $(this).attr('task'));
    toolbox.hide();

    var box = toolbox.children('.operator').clone(false);
    box.show();
    box.css({top: $(this).attr('posY'), left: $(this).attr('posX'), position:'absolute'});
    box.appendTo(editorContent);

    // Make operator draggable
    jsPlumb.draggable(box);

    // Add endpoints
    jsPlumb.addEndpoint(box, endpointSource);
    jsPlumb.addEndpoint(box, endpointTarget);
  });
}