
// Commit workflow xml to backend
function commitWorkflow() {
  $.ajax({
    type: 'PUT',
    url: apiUrl,
    contentType: 'text/xml',
    processData: false,
    data: serializeWorkflow(),
    success: function(response) {
    },
    error: function(req) {
      alert('Error committing workflow to backend: ' + req.responseText);
    }
  });
}

function serializeWorkflow() {
  // Create xml document
  var xmlDoc = document.implementation.createDocument('', 'root', null);
  var xml = xmlDoc.createElement("Workflow");
  // minPosition will be used to align the minimum x and y values to 0
  const minPosition = minPositionAllOperators();
  // Serialize all operators and datasets
  $('#editorContent').find('.operator').each(function() { serializeWorkflowOperator(this, xml, "Operator", minPosition) });
  $('#editorContent').find('.dataset').each(function() { serializeWorkflowOperator(this, xml, "Dataset", minPosition) });

  // Return xml string
  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  return xmlString;
}

// The minimum x and y value of all workflow operators
function minPositionAllOperators() {
  const xValues = $('#editorContent').find('.operator, .dataset').map(function() {
     var position = $(this).position();
     return position.left;
  });
  const yValues = $('#editorContent').find('.operator, .dataset').map(function() {
       var position = $(this).position();
       return position.top;
    });
  xValues.push(0);
  yValues.push(0);
  const minX = Math.min.apply(null, xValues);
  const minY = Math.min.apply(null, yValues);
  console.log(minX + ', ' + minY);
  return [minX, minY]
}

// type can be 'Operator' or 'Dataset'
function serializeWorkflowOperator(op, xml, type, minPosition) {
  const minX = minPosition[0];
  const minY = minPosition[1];
  // Get position
  var position = $(op).position();

  // Collect incoming and outgoing connections for this operator
  var incoming = jsPlumb.getConnections({ target: op.id });
  var outgoing = jsPlumb.getConnections({ source: op.id });

  // Create a list of source ids
  var sources =
      $.map(incoming, function(connection) {
        return connection.sourceId;
      }).join(",");

  // Create a list of target ids
  var targets =
      $.map(outgoing, function(connection) {
        return connection.targetId;
      }).join(",");

  // Assemble xml
  var operatorXml = xml.ownerDocument.createElement(type);
  operatorXml.setAttribute("posX", position.left - minX);
  operatorXml.setAttribute("posY", position.top - minY);
  var taskId = $(op).attr("taskid");
  if(taskId === undefined) {
    taskId = op.id
  }
  operatorXml.setAttribute("task", taskId);
  var outputPriority = $(op).attr("output_priority")
  if(outputPriority) {
    operatorXml.setAttribute("outputPriority", outputPriority);
  }
  operatorXml.setAttribute("id", op.id);
  operatorXml.setAttribute("inputs", sources);
  operatorXml.setAttribute("outputs", targets);
  xml.appendChild(operatorXml);
}