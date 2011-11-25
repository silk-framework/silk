/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var current_page = 1;
var fid;
contentWidthCallback = updateResultsWidth;

function useFilter(size) {
  clearTimeout(fid);
  fid = setTimeout("initPagination("+size+")", 2000);
}

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
  var navi_width = 94 + (number_results/100)*34;
  if (number_results < 101) navi_width = 124;
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

    if (confidence >= 0) {
      $(this).progressbar({
        value: confidence/2
      });
      $(this).children(".ui-progressbar-value").addClass("confidence-green").css("margin-left", "50%");
    } else {
      $(this).progressbar({
        value: -confidence/2
      });
      var left = 48 + confidence/2;
      $(this).children(".ui-progressbar-value").addClass("confidence-red").css("margin-left", left+"%");
    }

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

function updateResultsWidth() {
  var new_links_width = 326;
  if (contentWidth<600) {
    $("#results, #tree-header, #tree-footer").width(938);
    $(".link-source, .link-target").width(new_links_width);
    $(".middle").width(537);
    $("#wrapper").width(938);
  } else {
    new_links_width = (contentWidth-338)/2;
    $("#results, #tree-header, #tree-footer").width(contentWidth-54);
    $(".link-source, .link-target").width(new_links_width);
    $(".middle").width(contentWidth-455);
    $("#wrapper").width(contentWidth-54);
  }
  $(".link-source > a, .link-target > a").each(function(index) {
    if ($(this).width() > new_links_width) {
      $(this).css("float","right");
    } else {
      $(this).css("float","left");
    }
  });
}

$(function() {
  $("#selectLinks").buttonset();

  $(".link-header").live('click', function(e) {
    var link_id = $(this).parent().attr('id');
    if ($(e.target).is('a, img')) return;
    toggleLinkDetails(link_id);
  });
});
