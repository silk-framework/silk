/**
 * Serializes the current linkage rule in the editor as XML.
 */
function serializeLinkageRule() {
  var xml = serializeRule("LinkageRule");

  // Add filter
  var filter = xml.ownerDocument.createElement("Filter");
  var filterLimit = $("#linklimit").find(":selected").text();
  if (filterLimit != "unlimited") {
    filter.setAttribute("limit", filterLimit);
  }
  xml.appendChild(filter);

  // Add link type attribute
  xml.setAttribute("linkType", $("#linktype").val());

  return makeXMLString(xml);
}

/**
 * Serializes the current transformation rule in the editor as XML.
 */
function serializeTransformRule() {
  var xml = serializeRule("TransformRule");
  xml.setAttribute("name", $("#rulename").val());
  xml.setAttribute("targetProperty", $("#targetproperty").val());
  return makeXMLString(xml);
}

/**
 * Serializes the current rule as XML.
 */
function serializeRule(tagName) {
  // Retrieve all connections
  var connections = jsPlumb.getConnections({scope: ['value', 'similarity']}, true);
  // Find the root of the linkage rule
  var root = findRootOperator(connections);

  // Serialize rule
  var xmlDoc = document.implementation.createDocument('', 'root', null);
  var xml = xmlDoc.createElement(tagName);
  if (root != null) {
    xml.appendChild(parseOperator(xmlDoc, root, connections).xml);
  }

  return xml;
}

/**
 * Parses a single operator and returns the resulting XML.
 *
 * @return An object with two fields:
 *   - xml: The generated xml
 *   - inputType: The input type, either source or target. Undefined if the given operator is not an input
 */
function parseOperator(xmlDoc, elementId, connections) {
  var elementIdName = "#"+elementId;
  var elName = ($(elementIdName).children(".name").text());
  var elType = ($(elementIdName).children(".type").text());

  // Create xml element
  var xml;
  if (elType == "Source" || elType == "Target") {
    xml = xmlDoc.createElement("Input");
    var path = $(elementIdName+" > div.content > input").val();
    xml.setAttribute("path", path);
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
  var id = $(elementIdName + " .handler label").text();
  xml.setAttribute("id", id);

  // Parse children
  var children = [];
  for (var i = 0; i < connections.length; i++) {
    var source = connections[i].sourceId;
    var target = connections[i].targetId;
    if (target == elementId) {
      children.push(parseOperator(xmlDoc, source, connections));
    }
  }

  // Append children
  if(elType == "Compare") {
   // For comparisons, we need to append the children in the correct order
   if(children[0].inputType == "Source") {
     xml.appendChild(children[0].xml);
     if(children.length > 1)
       xml.appendChild(children[1].xml);
   } else {
     if(children.length > 1)
       xml.appendChild(children[1].xml);
     xml.appendChild(children[0].xml);
   }
  } else {
    for (var i in children) {
      xml.appendChild(children[i].xml)
    }
  }

  // If this is a path, we are finished. Otherwise the parameters still need to be parsed.
  if(elType == "Source" || elType == "Target")
    return { xml: xml, inputType: elType };

  // Parse parameters
  var params = $(elementIdName+" div.content input");

  for (var l = 0; l < params.length; l++) {
    if ($(params[l]).attr("name") == "required") {
      if ($(params[l]).is(':checked')) {
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

  return {
    xml: xml,
    // For now we just select the type of the first child
    // Todo verify for input trees that all do have the same input type
    inputType: children[0].elType
  };
}

/**
 * Finds the root of the rule tree
 */
function findRootOperator(connections) {
  // If there is only one element, it is the root
  var elements = $("#droppable").find("> div.dragDiv");
  if (elements.length === 1) {
    return elements.attr('id');
  }

  // Collect connection sources and targets
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

  return root;
}

/**
 * Generate XML string
 */
function makeXMLString(xml) {
  var xmlString = (new XMLSerializer()).serializeToString(xml);
  xmlString = xmlString.replace(/&amp;/g, "&");
  return xmlString;
}