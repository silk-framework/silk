var current_page = 1;

function handlePaginationClick(new_page_index, pagination_container) {
  showLinks(new_page_index);
  current_page = new_page_index;
  return false;
}

function initPagination(number_results) {
  $(".navigation").pagination(number_results, {
    items_per_page:100,
    callback:handlePaginationClick
  });
  var navi_width = 82 + (number_results/100)*34;
  if (number_results < 101) navi_width = 116;
  if (number_results > 1100) navi_width = 525;
  $(".navigation").css("width", navi_width + "px").css("float", "none").css("margin", "0 auto");
}

function initTrees() {
  $(".details-tree").treeview();

  // fix '+' and '-' icons:
  $("li.expandable").removeClass("expandable").addClass("collapsable");
  $("li.lastExpandable").removeClass("lastExpandable").addClass("lastCollapsable");
  $("div.expandable-hitarea").removeClass("expandable-hitarea").addClass("collapsable-hitarea");

  $(".confidencebar").each(function(index) {
    var confidence = parseInt($(this).text());
    var progressbar = $(this).children(".ui-progressbar-value");
    $(this).progressbar({
      value: confidence
    });
    if (confidence < 25) $(this).children(".ui-progressbar-value").addClass("confidence-red");
    if (confidence > 24 && confidence < 50) $(this).children(".ui-progressbar-value").addClass("confidence-orange");
    if (confidence > 49 && confidence < 75) $(this).children(".ui-progressbar-value").addClass("confidence-yellow");
    if (confidence > 74) $(this).children(".ui-progressbar-value").addClass("confidence-green");
  });
}

function toggleLinkDetails(linkid) {
  if ($("#details" + linkid).is(":visible")) {
    $("#toggle" + linkid + " > span").removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
    $("#details" + linkid).slideUp(300);
  } else {
    $("#toggle" + linkid + " > span").removeClass('ui-icon-triangle-1-e').addClass('ui-icon-triangle-1-s');
    $("#details" + linkid).slideDown(300);
  }
}

function expand_all() {
  $(".link-details").show();
}
function hide_all() {
  $(".link-details").hide();
}

$(function() {
  $("#selectLinks").buttonset();

  $(".link-header").live('click', function(e) {
    var link_id = $(this).parent().attr('id');
    if ($(e.target).is('a, img')) return;
    toggleLinkDetails(link_id);
  });

});