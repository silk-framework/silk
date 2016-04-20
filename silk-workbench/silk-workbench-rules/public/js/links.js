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

var path;
var linkType;
var sorting = 'unsorted';
var filter = '';
var page;

var fid;
contentWidthCallback = updateResultsWidth;

function initLinks(path, linkType) {
  this.path = path;
  this.linkType = linkType;

  $(document).ready(function() {
    page = 0;
    updateLinks(0);
  });
}

function updateLinkType(linkType) {
  this.linkType = linkType;
  updateLinks(0);
}

function updateSorting(sorting) {
  this.sorting = sorting;
  updateLinks(1000);
}

function updateFilter(filter) {
  this.filter = filter;
  updateLinks(1000);
}

function updatePage(page) {
  if(this.page != page) {
    this.page = page;
    updateLinks(0);
  }
}

function updateLinks(timeout) {
  if(timeout === undefined) {
    timeout = 2000;
  }
  $('#pending').show();
  clearTimeout(fid);
  if(timeout > 0) {
    fid = setTimeout('reloadLinks()', timeout);
  } else {
    reloadLinks();
  }
}

function reloadLinks() {
  $.get(path + '/' + linkType + '/' + sorting + '/filter:' + filter + '/' + page, function(data) {
    $('#links').html(data);
    initTrees();
    updateResultsWidth();
    $('#pending').hide();
  }).fail(function(request) { alert(request.responseText);  })
}

function handlePaginationClick(new_page_index, pagination_container) {
  updatePage(new_page_index);
  return false;
}

function initPagination(number_results) {
  $(".navigation").pagination(number_results, {
    items_per_page: 100,
    current_page: page,
    callback: handlePaginationClick
  });
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

  new_links_width = (contentWidth-338)/2;
  $("#results, #tree-header, #tree-footer").width(contentWidth-54);
  $(".link-source, .link-target").width(new_links_width);
  $(".middle").width(contentWidth-480);
  $("#wrapper").width(contentWidth-54);

  $(".link-source > a, .link-target > a").each(function(index) {
    if ($(this).width() > new_links_width) {
      $(this).css("float","right");
    } else {
      $(this).css("float","left");
    }
  });
}

$(function() {
  //$("#selectLinks").buttonset();

  $(document).on('click', ".link-header", function(e) {
    var link_id = $(this).parent().attr('id');
    if ($(e.target).is('a, img')) return;
    toggleLinkDetails(link_id);
  });
});

function deleteLink(id, source, target) {
  $.ajax({
    type: 'DELETE',
    url: apiUrl + '?source=' + source + '&target=' + target,
    data: '',
    success: function(response) {
      $('#' + id).remove();
    },
    error: function(request) { alert(request.responseText); }
  });
}

function resetLink(id, source, target) {
  $.ajax({
    type: 'DELETE',
    url: apiUrl + '?source=' + source + '&target=' + target,
    data: '',
    success: function(response) {
      $('#confirmedLink' + id).hide();
      $('#declinedLink' + id).hide();
      $('#undecidedLink' + id).show();
    },
    error: function(request) { alert(request.responseText); }
  });
}

function addPositiveLink(id, source, target) {
  $.ajax({
    type: 'PUT',
    url: apiUrl + '?linkType=positive&source=' + source + '&target=' + target,
    data: '',
    success: function(response) {
      $('#confirmedLink' + id).show();
      $('#declinedLink' + id).hide();
      $('#undecidedLink' + id).hide();
    },
    error: function(request) {
      alert(request.responseText);
    }
  });
}

function addNegativeLink(id, source, target) {
  $.ajax({
    type: 'PUT',
    url: apiUrl + '?linkType=negative&source=' + source + '&target=' + target,
    data: '',
    success: function(response) {
      $('#confirmedLink' + id).hide();
      $('#declinedLink' + id).show();
      $('#undecidedLink' + id).hide();
    },
    error: function(request) { alert(request.responseText); }
  });
}
