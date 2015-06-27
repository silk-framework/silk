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
    if($(this).hasClass("directMapping") || $(this).find(".source").lenght > 0) {
      serializeDirectMapping(xmlDoc, name, source, target)
    } else if($(this).hasClass("uriMapping")) {
      serializeUriMapping(xmlDoc, name, source)
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
  if(source.trim() != "") {
    var sourceXml = xmlDoc.createElement("Input");
    sourceXml.setAttribute("path", source);
    ruleXml.appendChild(sourceXml);
  }

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);
}

/**
 * Serializes a URI mapping.
 */
function serializeUriMapping(xmlDoc, name, source) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", target);

  // Add simple source
  if(source.trim() != "") {
    var sourceXml = xmlDoc.createElement("Input");
    sourceXml.setAttribute("path", source);
    ruleXml.appendChild(sourceXml);
  }

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