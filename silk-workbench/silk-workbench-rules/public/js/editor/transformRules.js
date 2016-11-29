var currentRule;
var confirmOnExit = false;
var modificationTimer;

$(function() {
  // Make rules sortable
  $("#ruleTable table").sortable({
    items: "> tbody"
  });
  //$("#typeContainer").sortable();

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
  addSourceAutocomplete($(".source"));
  addTargetAutocomplete($(".target"));

   // toggle URI mapping UI
   uriMappingExists() ? showURIMapping(true) : showURIMapping(false);
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
    var errors = duplicateNames.map(function(name) { return { type: "Error", message: "The following name is not unique: " + name }; } );
    updateStatus(errors);
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
      updateStatus([]);
    },
    error: function(req) {
      console.log('Error committing rule: ' + req.responseText);
      var errors = [ { type: "Error", message: req.responseText } ];
      updateStatus(errors);
    }
  });
}

function serializeRules() {
  var xmlDoc = $.parseXML('<TransformRules></TransformRules>');

  // Collect all rules
  $("#ruleContainer .transformRule").each(function() {
    // Read name
    var name = $(this).find(".rule-name").text();
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
      serializeTypeMapping(xmlDoc, name, $(this).find(".type").text())
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

function addType(typeString) {
  console.log(typeString);
}

function addURIMapping() {
  addRule("#uriMappingTemplate");
  $(".uri-ui").toggle();
}

function addRule(template) {

  if (template == "#typeTemplate") {
    var newRule = $(template + " .typeMapping").clone();
    var nameInput = newRule.find(".rule-name");
    nameInput.text(generateRuleName(nameInput.text()));
    var typeString = $("#rule-type-textfield input").val();
    newRule.find(".type").text(typeString);
    newRule.appendTo("#typeContainer");
    $("#rule-type-textfield input").val("");
  } else if(template == "#uriMappingTemplate") {
    var newRule = $(template).children().clone();
    resetMDLTextfields(newRule);
    newRule.appendTo(".uri-ui--defined");
  } else {
    // Clone rule template
    var newRule = $(template).children().clone();
    var nameInput = newRule.find(".rule-name");
    nameInput.text(generateRuleName(nameInput.text()));

    resetMDLTextfields(newRule);

    newRule.appendTo("#ruleTable table");
    $(".mdl-layout__content").animate({
      scrollTop: $("#content").height()
     }, 300);
  }

  componentHandler.upgradeAllRegistered();

  // Add autocompletion
  addSourceAutocomplete(newRule.find(".source"));
  addTargetAutocomplete(newRule.find(".target"));

  // Set modification flag
  modified();
}

function resetMDLTextfields(element) {
  // remove dynamic mdl classes and attributes
  // (otherwise componentHandler.upgradeAllRegistered() won't work)

  var textfields = element.find(".mdl-textfield");
  $.each(textfields, function(index, value) {
    value.removeAttribute("data-upgraded");
    var classes = value.className;
    var new_classes = classes.replace(/is-upgraded/, '').replace(/is-dirty/, '');
    value.className = new_classes;
  });
}

function deleteRule(node) {
  showDialog(baseUrl + '/transform/dialogs/deleteRule/' + encodeURIComponent(node));
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
    if($("#ruleContainer").find(".rule-name").filter(function() { return $(this).text() == prefix + count } ).length == 0) {
      return prefix + count;
    }
  } while (count < 1000);
}

function toggleRuleConfig() {
  var confContent = $("#ruleConfigContainer .mdl-card__supporting-text");
  var buttons = $("#ruleConfigContainer .mdl-card__title button");
  confContent.toggle(50, function() { buttons.toggle(); });
}

function toggleRule(ruleId) {
  var expandedRule = $("#" + ruleId + "__expanded");
  var buttons = $("#" + ruleId + " .rule-toggle button");
  expandedRule.toggle(50, function() { buttons.toggle(); });
}

function uriMappingExists() {
  return $(".uri-ui--defined").children()[0] != null;
}

function showURIMapping(defined) {
  if (defined) {
    $(".uri-ui--defined").show();
    $(".uri-ui--replacement").hide();
  } else {
    $(".uri-ui--defined").hide();
    $(".uri-ui--replacement").show();
  }
}

function addSourceAutocomplete(sourceInputs) {
  sourceInputs.autocomplete({
    source: apiUrl + "/sourcePathCompletions",
    minLength: 0,
    position: { my: "left bottom", at: "left top", collision: "flip" }
  }).focus(function() { $(this).autocomplete("search"); });
}

function addTargetAutocomplete(targetInputs) {
  targetInputs.each(function() {
    var sourceInput = $(this).closest("tr").find(".source");
    $(this).autocomplete({
      source: function( request, response ) {
        request.sourcePath = sourceInput.val();
        $.getJSON( apiUrl + "/targetPathCompletions", request, function(data) { response( data ) });
      },
      minLength: 0,
      position: { my: "left bottom", at: "left top", collision: "flip" }
    }).focus(function() { $(this).autocomplete("search"); });

    // Update the property details on every change
//    var details = $(this).closest(".complete-rule").find(".di-rule__expanded-property-details");
//    $.get(editorUrl + '/widgets/property', { property: $(this).val() }, function(data) { details.html(data); });
//    $(this).on("blur", function() {
//      $.get(editorUrl + '/widgets/property', { property: $(this).val() }, function(data) { details.html(data); });
//    });
  });
}
