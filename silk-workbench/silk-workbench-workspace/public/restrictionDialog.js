
function commitSparqlRestriction() {
  var sparql = $("textarea[name='sparql_restriction']").val();
  closeRestrictionDialog(sparql);
}