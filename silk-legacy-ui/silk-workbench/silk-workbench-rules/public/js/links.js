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

/* global contentWidth:true */

/* eslint-disable no-shadow */

var path;
var linkType;
var sorting = 'unsorted';
var filter = '';
var page;
var timeout = 2000;

var fid;
contentWidthCallback = updateResultsWidth;

/* exported initLinks
silk-workbench/silk-workbench-rules/app/views/generateLinks/generateLinks.scala.html
silk-workbench/silk-workbench-rules/app/views/referenceLinks/referenceLinks.scala.html
 */
function initLinks(newPath, newLinkType) {
    path = newPath;
    linkType = newLinkType;

    $(document).ready(function() {
        page = 0;
        updateLinks(0);
    });
}

/* exported updateLinkType
silk-workbench/silk-workbench-rules/app/views/referenceLinks/referenceLinks.scala.html
 */
function updateLinkType(newLinkType) {
    linkType = newLinkType;
    updateLinks(0);
}

/* exported updateSorting
silk-workbench/silk-workbench-rules/app/views/widgets/linksTable.scala.html
 */
function updateSorting(newSorting) {
    sorting = newSorting;
    updateLinks(1000);
}

/* exported updateFilter
silk-workbench/silk-workbench-rules/app/views/generateLinks/generateLinks.scala.html
silk-workbench/silk-workbench-rules/app/views/referenceLinks/referenceLinks.scala.html
 */
function updateFilter(newFilter) {
    filter = newFilter;
    updateLinks(1000);
}

function updatePage(newPage) {
    if (page !== newPage) {
        page = newPage;
        updateLinks(0);
    }
}

function updateLinks() {
    $('#pending').show();
    clearTimeout(fid);
    if (timeout > 0) {
        fid = setTimeout(reloadLinks, timeout);
    } else {
        reloadLinks();
    }
}

function reloadLinks() {
    $.get(`${path}/${linkType}/${sorting}/filter:${encodeURIComponent(filter)}/${page}`, function(
        data
    ) {
        $('#links').html(data);
        initTrees();
        updateResultsWidth();
        $('#pending').hide();
    }).fail(function(request) {
        alert(request.responseText);
    });
}

function handlePaginationClick(event, new_page_index) {
    // first page should be 0 not 1
    updatePage(new_page_index - 1);
    return false;
}

/* exported initPagination
silk-workbench/silk-workbench-rules/app/views/widgets/linksTable.scala.html
 */
function initPagination(number_results) {
    const $navigation = $('.navigation');
    $navigation.twbsPagination('destroy');
    $navigation.twbsPagination({
        // rounds up number of needed pages
        totalPages:
            Math.ceil(number_results / 100) === 0
                ? 1
                : Math.ceil(number_results / 100),
        visiblePages: 7,
        startPage: page + 1,
        onPageClick: handlePaginationClick,
        nextClass: 'mdl-button mdl-button--pagination next',
        prevClass: 'mdl-button mdl-button--pagination prev',
        lastClass: 'mdl-button mdl-button--pagination last',
        firstClass: 'mdl-button mdl-button--pagination first',
        pageClass: 'mdl-button mdl-button--pagination',
        activeClass: 'mdl-button--active',
    });
}

function initTrees() {
    $('.details-tree').treeview();

    // fix '+' and '-' icons:
    $('li.expandable')
        .removeClass('expandable')
        .addClass('collapsable');
    $('li.lastExpandable')
        .removeClass('lastExpandable')
        .addClass('lastCollapsable');
    $('div.expandable-hitarea')
        .removeClass('expandable-hitarea')
        .addClass('collapsable-hitarea');

    $('.confidencebar').each(function() {
        var confidence = parseInt($(this).text(), 10);

        if (confidence >= 0) {
            $(this).progressbar({
                value: confidence / 2,
            });
            $(this)
                .children('.ui-progressbar-value')
                .addClass('confidence-green')
                .css('margin-left', '50%');
        } else {
            $(this).progressbar({
                value: -confidence / 2,
            });
            var left = 48 + confidence / 2;
            $(this)
                .children('.ui-progressbar-value')
                .addClass('confidence-red')
                .css('margin-left', `${left}%`);
        }
    });
}

function toggleLinkDetails(linkid) {
    if ($(`#details${linkid}`).is(':visible')) {
        $(`#toggle${linkid} > span`)
            .removeClass('ui-icon-triangle-1-s')
            .addClass('ui-icon-triangle-1-e');
        $(`#details${linkid}`).slideUp(300);
    } else {
        $(`#toggle${linkid} > span`)
            .removeClass('ui-icon-triangle-1-e')
            .addClass('ui-icon-triangle-1-s');
        $(`#details${linkid}`).slideDown(300);
    }
}

/* exported expand_all hide_all
silk-workbench/silk-workbench-rules/app/views/generateLinks/generateLinks.scala.html
silk-workbench/silk-workbench-rules/app/views/referenceLinks/referenceLinks.scala.html
 */
function expand_all() {
    $('.link-details').show();
}
function hide_all() {
    $('.link-details').hide();
}

function updateResultsWidth() {
    var new_links_width = 326;

    new_links_width = (contentWidth - 338) / 2;
    $('#results, #tree-header, #tree-footer').width(contentWidth - 54);
    $('.link-source, .link-target').width(new_links_width);
    $('.middle').width(contentWidth - 480);
    $('#wrapper').width(contentWidth - 54);

    $('.link-source > a, .link-target > a').each(function() {
        if ($(this).width() > new_links_width) {
            $(this).css('float', 'right');
        } else {
            $(this).css('float', 'left');
        }
    });
}

$(function() {
    $(document).on('click', '.link-header', function(e) {
        var link_id = $(this)
            .parent()
            .attr('id');
        if ($(e.target).is('a, img')) return;
        toggleLinkDetails(link_id);
    });
});

/* exported deleteLink
silk-workbench/silk-workbench-rules/app/views/widgets/linkButtons.scala.html
 */
function deleteLink(id, source, target) {
    $.ajax({
        type: 'DELETE',
        url: `${apiUrl}?source=${source}&target=${target}`,
        data: '',
        success() {
            $(`#${id}`).remove();
        },
        error(request) {
            alert(request.responseText);
        },
    });
}

/** Show the buttons in link evaluation */
function showButtons(cssSelector) {
    $(cssSelector).attr('class', 'displayAsBlock');
}

function hideButtons(cssSelector) {
    $(cssSelector).attr('class', 'displayAsNone');
}

function resetLink(id, source, target) {
    $.ajax({
        type: 'DELETE',
        url: `${apiUrl}?source=${source}&target=${target}`,
        data: '',
        success() {
            hideButtons(`#confirmedLink${id}`);
            hideButtons(`#declinedLink${id}`);
            showButtons(`#undecidedLink${id}`);
        },
        error(request) {
            alert(request.responseText);
        },
    });
}

/* exported addPositiveLink
silk-workbench/silk-workbench-rules/app/views/widgets/linkButtons.scala.html
 */
function addPositiveLink(id, source, target) {
    $.ajax({
        type: 'PUT',
        url: `${apiUrl}?linkType=positive&source=${source}&target=${target}`,
        data: '',
        success() {
            showButtons(`#confirmedLink${id}`);
            hideButtons(`#declinedLink${id}`);
            hideButtons(`#undecidedLink${id}`);
        },
        error(request) {
            alert(request.responseText);
        },
    });
}

/* exported addNegativeLink
silk-workbench/silk-workbench-rules/app/views/widgets/linkButtons.scala.html
 */
function addNegativeLink(id, source, target) {
    $.ajax({
        type: 'PUT',
        url: `${apiUrl}?linkType=negative&source=${source}&target=${target}`,
        data: '',
        success() {
            hideButtons(`#confirmedLink${id}`);
            showButtons(`#declinedLink${id}`);
            hideButtons(`#undecidedLink${id}`);
        },
        error(request) {
            alert(request.responseText);
        },
    });
}