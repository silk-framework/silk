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
    // Read target
    var target = $(this).find(".target").val();
    // Check if the source path is set
    var sourceInput = $(this).find(".source");
    if(sourceInput.length > 0) { // Source path is set => Create a simple rule
      serializeSimpleRule(xmlDoc, name, sourceInput.val(), target)
    } else { // Source path is not set => Keep complex rule
      var ruleXml = $.parseXML($(this).children('.ruleXML').text()).documentElement;
      serializeComplexRule(xmlDoc, ruleXml, name, target)
    }
  });

  // Push to back-end
  var xmlString = (new XMLSerializer()).serializeToString(xmlDoc);
  return xmlString;
}

/**
 * Serializes a simple rule.
 * A simple rule is a 1-to-1 mapping between two properties
 */
function serializeSimpleRule(xmlDoc, name, source, target) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", target);

  // Add simple source
  if(source.trim() != "") {
    var sourceXml = xmlDoc.createElement("Input");
    if (source.startsWith("?"))
      sourceXml.setAttribute("path", source);
    else
      sourceXml.setAttribute("path", "?a/" + source);
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

function addRule() {
  // Clone rule template
  var newRule = $("#ruleTemplate").children().clone();
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