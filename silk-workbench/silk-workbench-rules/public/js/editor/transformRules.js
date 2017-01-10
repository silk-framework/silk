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
      var nodeType = $(this).find(".target-type").val();
      serializeDirectMapping(xmlDoc, name, source, target, nodeType);
    } else if($(this).hasClass("uriMapping")) {
      serializeUriMapping(xmlDoc, name, $(this).find(".pattern").val());
    } else if($(this).hasClass("objectMapping")) {
      serializeObjectMapping(xmlDoc, name, $(this).find(".pattern").val(), target);
    } else if($(this).hasClass("typeMapping")) {
      serializeTypeMapping(xmlDoc, name, $(this).find(".type").text());
    } else {
      var ruleXml = $.parseXML($(this).children('.ruleXML').text()).documentElement;
      serializeComplexRule(xmlDoc, ruleXml, name, target);
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
function serializeDirectMapping(xmlDoc, name, source, target, nodeType) {
  // Create new rule
  var ruleXml = xmlDoc.createElement("TransformRule");
  ruleXml.setAttribute("name", name);
  ruleXml.setAttribute("targetProperty", target);
  //ruleXml.setAttribute("targetType", type);

  // Add simple source
  var sourceXml = xmlDoc.createElement("Input");
  sourceXml.setAttribute("path", source);
  ruleXml.appendChild(sourceXml);


  // Add MappingTarget
  var mappingType = xmlDoc.createElement("MappingTarget");
  var valueType = xmlDoc.createElement("ValueType");
  valueType.setAttribute("nodeType", nodeType);
  mappingType.appendChild(valueType);
  ruleXml.appendChild(mappingType);

  // Add to document
  xmlDoc.documentElement.appendChild(ruleXml);

  console.log(ruleXml);
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
    var ruleName = generateRuleName(nameInput.text());
    nameInput.text(ruleName);
    var ruleId = "type-" + ruleName;
    newRule.attr("id", ruleId);
    var typeString = $("#rule-type-textfield input").val();
    newRule.find(".type").text(typeString);
    newRule.appendTo("#typeContainer");
    $("#rule-type-textfield input").val("");
    var deleteButton = newRule.find("button");
    deleteButton.attr("onclick", "deleteRule('" + ruleId + "');");
  } else if(template == "#uriMappingTemplate") {
    var newRule = $(template).children().clone();
    resetMDLTextfields(newRule);
    newRule.appendTo(".uri-ui--defined");
  } else {
    // Clone rule template
    var newRule = $(template).children().clone();

    var nameInput = newRule.find(".rule-name");
    var oldRuleName = nameInput.text();
    var newRuleName = generateRuleName(oldRuleName);
    nameInput.text(newRuleName);

    var ruleRows = newRule.find("tr");
    $.each(ruleRows, function(index, row) {
      row = $(row);
      var ruleId = row.attr("id");
      ruleId = ruleId.replace(oldRuleName, newRuleName);
      row.attr("id", ruleId);
      row.find("button.delete-button").attr("onclick", "deleteRule('" + ruleId + "');");
    })

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

function checkForEmptyURIMapping() {
  var pattern = $("#uri-pattern").val();
  if (pattern.match(/^\s*$/)) { // if empty or only whitespace
    $('#uri').remove();
    showURIMapping(false);
    modified();
  }
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
      position: { my: "left bottom", at: "left top", collision: "flip" } ,
      close: function(event, ui) { modified(); } ,
      focus: function(event, ui) { changePropertyDetails(ui.item.value, $(this));}
    }).focus(function() { $(this).autocomplete("search"); });

    // Update the property details on every change
    changePropertyDetails($(this).val(), $(this));
    $(this).keyup(function() {
      changePropertyDetails($(this).val(), $(this));
    });
  });
}

function addTypeSelections(typeSelects) {
  var types = [
    { label: "Autodetect", value: "AutoDetectValueType$", category: "" } ,
    { label: "Resource", value: "UriValueType$", category: "" } ,
    { label: "Boolean", value: "BooleanValueType$", category: "Literals" } ,
    { label: "String", value: "StringValueType$", category: "Literals" } ,
    { label: "Integer", value: "IntegerValueType$", category: "Literals (Numbers)" } ,
    { label: "Long", value: "LongValueType$", category: "Literals (Numbers)" } ,
    { label: "Float", value: "FloatValueType$", category: "Literals (Numbers)" } ,
    { label: "Double", value: "DoubleValueType$", category: "Literals (Numbers)" } ,
  ];

  // fill the select lists
  var currentCategory = "";
  var target = typeSelects;
  $.each(types, function(index, value) {
    if ( value.category != currentCategory ) {
      currentCategory = value.category;
      typeSelects.append("<optgroup label='" + currentCategory + "'/>");
      target = typeSelects.find("optgroup:last-child");
    }
    target.append("<option value='" + value.value + "'>" + value.label + "</option>");
  });

  // select correct element
  $.each(typeSelects, function(index, value) {
    var targetType = $(value).data('originalTargetType');
    $(value).val(targetType + "$");
  });

  // register changes
  typeSelects.change(function() {
    modified();
  });
}

function addTypeAutocomplete(typeInputs) {
  $.widget( "custom.catcomplete", $.ui.autocomplete, {
    _create: function() {
      this._super();
      this.widget().menu( "option", "items", "> :not(.ui-autocomplete-category)" );
    },
    _renderMenu: function( ul, items ) {
      var that = this,
        currentCategory = "";
      $.each( items, function( index, item ) {
        var li;
        if ( item.category != currentCategory ) {
          ul.append( "<li class='ui-autocomplete-category'>" + item.category + "</li>" );
          currentCategory = item.category;
        }
        li = that._renderItemData( ul, item );
        if ( item.category ) {
          li.attr( "aria-label", item.category + " : " + item.label );
        }
      });
    }
  });
  typeInputs.catcomplete({
    source: [
      { label: "Autodetect", category: "" } ,
      { label: "Boolean", category: "" } ,
      { label: "String", category: "Strings" } ,
      { label: "Language String", category: "Strings" } ,
      { label: "Integer", category: "Numbers" } ,
      { label: "Float", category: "Numbers" } ,
      { label: "Double", category: "Numbers" } ,
      { label: "Date", category: "Dates" } ,
      { label: "Time", category: "Dates" } ,
      { label: "DateTime", category: "Dates" } ,
      { label: "Duration", category: "Dates" } ,
      { label: "Year", category: "Dates" } ,
      { label: "Month", category: "Dates" } ,
      { label: "Day", category: "Dates" } ,
      { label: "Year-Month", category: "Dates" } ,
      { label: "Month-Day", category: "Dates" } ,

//      "xsd:string" ,
//      "xsd:boolean" ,
//      "xsd:float" ,
//      "xsd:double" ,
//      "xsd:integer" ,
//      "xsd:date" ,
//      "xsd:time" ,
//      "xsd:duration" ,
//      "xsd:dateTime" ,
//      "xsd:gYear" ,
//      "xsd:gYearMonth" ,
//      "xsd:gMonthDay" ,
//      "xsd:gDay" ,
//      "xsd:gMonth" ,

    ] ,
    minLength: 0,
    position: { my: "left bottom", at: "left top", collision: "flip" } ,
  }).focus(function() { $(this).catcomplete("search"); });
}

//function addTargetTypeAutocomplete(inputs) {
//  inputs.each(function() {
//    $(this).autocomplete({
//      minLength: 0
//    })
//  })
//}

function changePropertyDetails(propertyName, element) {
  var details = element.closest(".complete-rule").find(".di-rule__expanded-property-details");
  $.get(editorUrl + '/widgets/property', { property: propertyName }, function(data) { details.html(data); });
}
