var current_page = 1;

function handlePaginationClick(new_page_index, pagination_container) {
  showLinks(new_page_index);
  current_page = new_page_index;
  return false;
}

function initPagination(number_pages) {
  $(".navigation").pagination(number_pages, {
    items_per_page:100,
    callback:handlePaginationClick
  });
}

function initTrees() {
  $(".details-tree").treeview();

  // fix '+' and '-' icons:
  $("li.expandable").removeClass("expandable").addClass("collapsable");
  $("li.lastExpandable").removeClass("lastExpandable").addClass("lastCollapsable");
  $("div.expandable-hitarea").removeClass("expandable-hitarea").addClass("collapsable-hitarea");

  $(".confidencebar").each(function(index) {
    $(this).progressbar({
      value: parseInt($(this).text())
    });
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