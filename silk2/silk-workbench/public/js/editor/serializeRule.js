/**
 * Serializes the current linkage rule in the editor as XML.
 */
function serializeLinkSpec() {
  // Retrieve all connections
  var connections = jsPlumb.getConnections({scope: ['value', 'similarity']}, true);

  // Collect connections sources and target
  var sources = [];
  var targets = [];
  for (var i = 0; i < connections.length; i++) {
    var source = connections[i].sourceId;
    var target = connections[i].targetId;
    sources[target] = source;
    targets[source] = target;
  }

  // Find root operator
  var root = null;
  for (var key in sources) {
    if (!targets[key]) {
      root = key;
    }
  }

  var xmlDoc = document.implementation.createDocument('', 'root', null);

  var xml = xmlDoc.createElement("LinkageRule");
  if ((root != null) && (connections != "")) {
    xml.appendChild(parseOperator(xmlDoc, root));
  }

  // TODO add to linkage rule
//  var filter = xmlDocument.createElement("Filter");
//  if ($("#linklimit :selected").text() != "unlimited") {
//    filter.setAttribute("limit", $("#linklimit :selected").text());
//  }
//  xml.appendChild(filter);

  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  return xmlString;
}

/**
 * Parses a single operator and returns the resulting XML.
 */
function parseOperator(xmlDoc, elementId) {
  var elementIdName = "#"+elementId;
  var elName = ($(elementIdName).children(".name").text());
  var elType = ($(elementIdName).children(".type").text());

  // Create xml element
  var xml;
  if (elType == "Source" || elType == "Target") {
    xml = xmlDoc.createElement("Input");
    var path = $(elementIdName+" > div.content > input").val();
    xml.setAttribute("path", encodeHtml(path));
  } else if (elType == "Transform") {
    xml = xmlDoc.createElement("TransformInput");
    xml.setAttribute("function", elName);
  } else if (elType == "Aggregate") {
    xml = xmlDoc.createElement("Aggregate");
    xml.setAttribute("type", elName);
  } else if (elType == "Compare") {
    xml = xmlDoc.createElement("Compare");
    xml.setAttribute("metric", elName);
  } else {
    alert("Unknown operator type: " + elType);
  }

  // Parse id
  var id = $(elementIdName + " > .label").text();
  if (!id) pluginId = $(id + " > div.label-active > input.label-change").val();
  xml.setAttribute("id", id);

  // Parse children
  var connections = jsPlumb.getConnections({scope: ['value', 'similarity']}, true);
  for (var i = 0; i < connections.length; i++) {
    var source = connections[i].sourceId;
    var target = connections[i].targetId;
    if (target == elementId) {
      xml.appendChild(parseOperator(xmlDoc, source));
    }
  }

  // If this is a path, we are finished. Otherwise the parameters still need to be parsed.
  if(elType == "Source" || elType == "Target")
    return xml;

  // Parse parameters
  var params = $(elementIdName+" > div.content > input");

  for (var l = 0; l < params.length; l++) {
    if ($(params[l]).attr("name") == "required") {
      if (($(elementIdName+" > div.content > input[name=required]:checked").val()) == "on") {
        xml.setAttribute("required", "true");
      } else {
        xml.setAttribute("required", "false");
      }
    } else if ($(params[l]).attr("name") == "threshold") {
      xml.setAttribute("threshold", $(params[l]).val());
    } else if ($(params[l]).attr("name") == "weight") {
      xml.setAttribute("weight", $(params[l]).val());
    } else {
      if (elType == "Compare") {
        if ($(params[l]).val() != "") {
          var xml_param = xmlDoc.createElement("Param");
          xml_param.setAttribute("name", $(params[l]).attr("name"));
          xml_param.setAttribute("value", $(params[l]).val());
          xml.appendChild(xml_param);
        }
      } else {
        var xml_param = xmlDoc.createElement("Param");
        xml_param.setAttribute("name", $(params[l]).attr("name"));
        xml_param.setAttribute("value", $(params[l]).val());
        xml.appendChild(xml_param);
      }
    }
  }

  return xml;
}