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
  $(document).on('keyup', "input", function() {
    modified();
  });

  $("#ruleContainer").on("sortupdate", function( event, ui ) { modified() } );
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
    // Parse original transform rule
    var ruleXml = $.parseXML($(this).children('.ruleXML').text()).documentElement;
    // Update name
    ruleXml.setAttribute("name", $(this).find("input").val());
    // Add to document
    xmlDoc.importNode(ruleXml, true);
    xmlDoc.documentElement.appendChild(ruleXml);
  });

  // Push to back-end
  var xmlString = (new XMLSerializer()).serializeToString(xmlDoc);
  return xmlString;
}

function addRule() {
  var newRule = $("#ruleTemplate").children().clone();
  newRule.appendTo("#ruleContainer");
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