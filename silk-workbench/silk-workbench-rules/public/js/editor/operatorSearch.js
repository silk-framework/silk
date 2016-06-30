// filtering of operator list based on search term

$('#operator_search_term').keyup(function(){
  var searchTerm = $(this).val().toLowerCase();
  if(searchTerm == "") {
    // show regular grouped view if search term empty
    $('#operators-grouped').show();
    $('#operators-search-result').hide();
  } else {
    // show ungrouped list of operators if we have a non-empty search term
    $('#operators-grouped').hide();
    $('#operators-search-result').show();
    $('#operatorList .operator').each(function() {
      var text = $(this).find('.operator-index').text().toLowerCase(); // the operator's index terms to match against
      if (text.indexOf(searchTerm) >= 0) {
        $(this).removeClass('search-invisible');
        $("#operators-search-result .operator p").unmark();
        if (searchTerm.length > 1) { // to improve performance, only highlight for searchTerms longer than 1
          $("#operators-search-result .operator p").mark(searchTerm, {"exclude": [ ".search-invisible" ] });
        }
      } else {
        $(this).addClass('search-invisible');
      }
    });
  };
});
