
// Commit workflow xml to backend
function commitWorkflow() {
  $.ajax({
    type: 'PUT',
    url: apiUrl,
    contentType: 'text/xml',
    processData: false,
    data: serializeWorkflow(),
    //dataType: "json",
    success: function(response) {
    },
    error: function(req) {
      console.log('Error committing rule: ' + req.status);
      alert(req.responseText)
    }
  });
}

function serializeWorkflow() {
  // Create xml document
  var xmlDoc = document.implementation.createDocument('', 'root', null);
  var xml = xmlDoc.createElement("Workflow");

  // Iterate through all operators
  $('#editorContent').find('.operator').each(function(i, obj) {
    // Get position
    var position = $(this).position();

    // Collect incoming and outgoing connections for this operator
    var incoming = jsPlumb.getConnections({ target: this.id });
    var outgoing = jsPlumb.getConnections({ source: this.id });

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
    var operatorXml = xmlDoc.createElement("WorkflowOperator");
    operatorXml.setAttribute("posX", position.left);
    operatorXml.setAttribute("posY", position.top);
    operatorXml.setAttribute("task", getId(this.id));
    operatorXml.setAttribute("inputs", sources);
    operatorXml.setAttribute("outputs", targets);
    xml.appendChild(operatorXml);
  });

  // Return xml string
  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  console.log("XML: " + xmlString);
  return xmlString;
}

function getId(id) {
  return id.substring(id.indexOf("_") + 1)
}
