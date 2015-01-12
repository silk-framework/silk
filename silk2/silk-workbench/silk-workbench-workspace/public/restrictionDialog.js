
$(function() {
  $("#tabs").tabs();
  $("button").button();
});

function commitSimpleRestriction(sourceVar, targetVar) {
  var sparql =
      $(".type.selected").map(function() {
        // Retrieve type name
        var name = $(this).attr("title");
        console.log("Title: " + name);
        // Enclose full URIs with brackets
        if(name.indexOf("/") != -1)
          name = "<" + name + ">";
        // Determine variable
        var variable;
        if(sourceOrTarget == "source")
          variable = sourceVar;
        else
          variable = targetVar;
        // Return SPARQL pattern
        return "{ ?" + variable + " a " + name + " }";
      }).toArray().join("\n UNION \n");

  closeRestrictionDialog(sparql);
}

function commitSparqlRestriction() {
  var sparql = $("textarea[name='sparql_restriction']").val();
  closeRestrictionDialog(sparql);
}