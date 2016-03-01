var currentRule;
var confirmOnExit = false;
var modificationTimer;

$(function() {
  // Make rules sortable
  $("#ruleContainer").sortable();

  // Initialize deletion dialog
  $("#dialogDelete").dialog({
    autoOpen: false,
    modal: true,
    buttons: {
      Yes: function() {
        currentRule.remove();
        modified();
        $(this).dialog("close");
      },
      Cancel: function() {
        $(this).dialog("close");
      }
    }
  });

  // Listen to modifications
  $(document).on('input', "input", function() {
    modified();
  });

  $("#ruleContainer").on("sortupdate", function( event, ui ) { modified() } );

  // Add autocompletion
   $(".source").autocomplete({
     source: apiUrl + "/sourcePathCompletions",
     minLength: 0
   }).focus(function() { $(this).autocomplete("search"); });

   $(".target").autocomplete({
    source: apiUrl + "/targetPathCompletions",
    minLength: 0
   }).focus(function() { $(this).autocomplete("search"); });
});

function modified() {
  confirmOnExit = true;
  showPendingIcon();
  clearTimeout(modificationTimer);
  modificationTimer = setTimeout(function() { save(); }, 2000);
}

function save() {
  clearTimeout(modificationTimer);

  // Check if rule names are unique
  // TODO set id and implement highlightElement
  var names = $("#ruleContainer").find(".name").map(function() { return $(this).val() } ).toArray();
  var duplicateNames = $.grep(names, function(v, i) { return $.inArray(v, names) != i });
  if(duplicateNames.length > 0) {
    var errors = duplicateNames.map(function(name) { return { message: "The following name is not unique: " + name }; } );
    updateStatus(errors, null, null);
    return;
  }

  // Commit rules
  $.ajax({
    type: 'PUT',
    url: apiUrl + '/rules',
    contentType: 'text/xml',
    processData: false,
    data: serializeRules(),
    success: function(response) {
      confirmOnExit = false;
      updateStatus(null, null, null);
    },
    error: function(req) {
      console.log('Error committing rule: ' + req.responseText);
      var errors = [ { message: req.responseText } ];
      updateStatus(errors, null, null);
    }
  });
}

function serializeRules() {
  var xmlDoc = $.parseXML('<TransformRules></TransformRules>');

  // Collect all rules
  $("#ruleContainer").children(".transformRule").each(function() {
    // Read name
    var name = $(this).find(".name").val();
    // Read source and target
    var source = $(this).find(".source").val();
    var target = $(this).find(".target").val();
    if($(this).hasClass("directMapping")) {
      serializeDirectMapping(xmlDoc, name, source, target)
    } else if($(this).hasClass("uriMapping")) {
      serializeUriMapping(xmlDoc, name, $(this).find(".pattern").val())
    } else if($(this).hasClass("objectMapping")) {
      serializeObjectMapping(xmlDoc, name, $(this).find(".pattern").val(), target)
    } else if($(this).hasClass("typeMapping")) {
      serializeTypeMapping(xmlDoc, name, $(this).find(".type").val())
    } else {
      var ruleXml = $.parseXML($(this).children('.ruleXML').text()).documentElement;
      serializeComplexRule(xmlDoc, ruleXml, name, target)
    }
  });

  // Push to back-end
  var xmlString = (new XMLSerializer()).serializeToString(xmlDoc);
  return xmlString;
}

/**
 * Serializes a direct mapping.
 * A direct mapping is a 1-to-1 mapping between two properties
 */
function serializeDirectMapping(xmlDoc, name, source, target) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", target);

  // Add simple source
  var sourceXml = xmlDoc.createElement("Input");
  sourceXml.setAttribute("path", source);
  ruleXml.appendChild(sourceXml);

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);
}

/**
 * Serializes a URI mapping.
 */
function serializeUriMapping(xmlDoc, name, pattern) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", "");

  // Create concat transformer
  var concatXml = xmlDoc.createElement("TransformInput");
  concatXml.setAttribute("function", "concat");
  ruleXml.appendChild(concatXml);

  // Parse pattern
  var parts = pattern.split(/[\{\}]/);
  for (i = 0; i < parts.length; i++) {
    if (i % 2 == 0) {
      // Add constant
      var transformXml = xmlDoc.createElement("TransformInput");
      transformXml.setAttribute("function", "constant");
      var paramXml = xmlDoc.createElement("Param");
      paramXml.setAttribute("name", "value");
      paramXml.setAttribute("value", parts[i]);
      transformXml.appendChild(paramXml);
      concatXml.appendChild(transformXml);
    } else {
      // Add path
      var inputXml = xmlDoc.createElement("Input");
      inputXml.setAttribute("path", parts[i]);
      concatXml.appendChild(inputXml);
    }
  }

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);
}

/**
 * Serializes a Object mapping.
 */
function serializeObjectMapping(xmlDoc, name, pattern, target) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", target);

  // Create concat transformer
  var concatXml = xmlDoc.createElement("TransformInput");
  concatXml.setAttribute("function", "concat");
  ruleXml.appendChild(concatXml);

  // Parse pattern
  var parts = pattern.split(/[\{\}]/);
  for (i = 0; i < parts.length; i++) {
    if (i % 2 == 0) {
      // Add constant
      var transformXml = xmlDoc.createElement("TransformInput");
      transformXml.setAttribute("function", "constant");
      var paramXml = xmlDoc.createElement("Param");
      paramXml.setAttribute("name", "value");
      paramXml.setAttribute("value", parts[i]);
      transformXml.appendChild(paramXml);
      concatXml.appendChild(transformXml);
    } else {
      // Add path
      var inputXml = xmlDoc.createElement("Input");
      inputXml.setAttribute("path", parts[i]);
      concatXml.appendChild(inputXml);
    }
  }

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);
}

/**
 * Serializes a type mapping.
 */
function serializeTypeMapping(xmlDoc, name, type) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

  // Input is the constant type URI
  var transformXml = xmlDoc.createElement("TransformInput");
  transformXml.setAttribute("function", "constantUri");

  var paramXml = xmlDoc.createElement("Param");
  paramXml.setAttribute("name", "value");
  paramXml.setAttribute("value", type);

  transformXml.appendChild(paramXml);
  ruleXml.appendChild(transformXml);

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);
}

/**
 * Serializes a complex rule.
 * For complex rules the rule contents are left untouched.
 */
function serializeComplexRule(xmlDoc, ruleXml, name, target) {
  // Update name
  ruleXml.setAttribute("name", name);
  // Update target
  ruleXml.setAttribute("targetProperty", target);
  // Add to document
  xmlDoc.importNode(ruleXml, true);
  xmlDoc.documentElement.appendChild(ruleXml);
}

function addRule(template) {
  // Clone rule template
  var newRule = $(template).children().clone();
  var nameInput = newRule.find(".name");
  nameInput.val(generateRuleName(nameInput.val()));
  newRule.appendTo("#ruleContainer");

  // Add autocompletion
  newRule.find(".source").autocomplete({
    source: apiUrl + "/sourcePathCompletions",
    minLength: 0
  }).focus(function() { $(this).autocomplete("search"); });

  newRule.find(".target").autocomplete({
    source: apiUrl + "/targetPathCompletions",
    minLength: 0
  }).focus(function() { $(this).autocomplete("search"); });

  // Set modification flag
  modified();
}

function deleteRule(node) {
  // Remember rule
  currentRule = node;
  // Show confirmation dialog
  $("#dialogDelete").dialog("open");
}

function openRule(name) {
  clearTimeout(modificationTimer);
  $.ajax({
    type: 'PUT',
    url: apiUrl + '/rules',
    contentType: 'text/xml',
    processData: false,
    data: serializeRules(),
    success: function(response) {
      window.location.href = "./editor/" + name
    },
    error: function(req) {
      console.log('Error committing rule: ' + req.responseText);
      alert(req.responseText);
    }
  });
}

function generateRuleName(prefix) {
  var count = 0;
  do {
    count = count + 1;
    if($("#ruleContainer").find(".name").filter(function() { return $(this).val() == prefix + count } ).length == 0) {
      return prefix + count;
    }
  } while (count < 1000);
}