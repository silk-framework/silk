
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

  // Serialize all operators and datasets
  $('#editorContent').find('.operator').each(function() { serializeOperator(this, xml) });
  $('#editorContent').find('.dataset').each(function() { serializeDataset(this, xml) });

  // Return xml string
  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  return xmlString;
}

function serializeOperator(op, xml) {
  // Get position
  var position = $(op).position();

  // Collect incoming and outgoing connections for this operator
  var incoming = jsPlumb.getConnections({ target: op.id });
  var outgoing = jsPlumb.getConnections({ source: op.id });

  // Create a list of source ids
  var sources =
      $.map(incoming, function(connection) {
        return getId(connection.sourceId);
      }).join(",");

  // Create a list of target ids
  var targets =
      $.map(outgoing, function(connection) {
        return getId(connection.targetId);
      }).join(",");

  // Assemble xml
  var operatorXml = xml.ownerDocument.createElement("Operator");
  operatorXml.setAttribute("posX", position.left);
  operatorXml.setAttribute("posY", position.top);
  var taskId = $(op).attr("taskid");
  if(taskId === undefined) {
    taskId = op.id
  }
  operatorXml.setAttribute("task", getId(taskId));
  operatorXml.setAttribute("id", op.id);
  operatorXml.setAttribute("inputs", sources);
  operatorXml.setAttribute("outputs", targets);
  xml.appendChild(operatorXml);
}

function serializeDataset(ds, xml) {
  var position = $(ds).position();
  var datasetXml = xml.ownerDocument.createElement("Dataset");
  datasetXml.setAttribute("posX", position.left);
  datasetXml.setAttribute("posY", position.top);
  var taskId = $(ds).attr("taskid");
  if(taskId === undefined) {
    taskId = ds.id
  }
  datasetXml.setAttribute("task", getId(taskId));
  xml.appendChild(datasetXml);
}

function getId(id) {
  return id.substring(id.indexOf("_") + 1)
}
