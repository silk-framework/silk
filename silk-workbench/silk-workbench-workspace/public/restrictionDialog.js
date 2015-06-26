
$(function() {
  $("#tabs").tabs();
  $("button").button();
});

function commitSimpleRestriction(varName) {
  var sparql =
      $(".type.selected").map(function() {
        // Retrieve type name
        var name = $(this).attr("title");
        console.log("Title: " + name);
        // Enclose full URIs with brackets
        if(name.indexOf("/") != -1)
          name = "<" + name + ">";
        // Return SPARQL pattern
        return "{ ?" + varName + " a " + name + " }";
      }).toArray().join("\n UNION \n");

  closeRestrictionDialog(sparql);
}

function commitSparqlRestriction() {
  var sparql = $("textarea[name='sparql_restriction']").val();
  closeRestrictionDialog(sparql);
}