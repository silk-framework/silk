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

contentWidthCallback = updateResultsWidth;

/* exported initPopulation
silk-workbench/silk-workbench-rules/app/views/learning/population.scala.html
 */
function initPopulation(path) {
    $.get(`${path}/0`, function(data) {
        $('#populationView').html(data);
        initTrees();
        updateResultsWidth();
    }).fail(function(request) {
        alert(request.responseText);
    });
}

function initTrees() {
    $('.details-tree').treeview();

    // fix '+' and '-' icons:
    $('li.expandable').removeClass('expandable').addClass('collapsable');
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
silk-workbench/silk-workbench-rules/app/views/learning/population.scala.html
 */
function expand_all() {
    $('.individual-details').show();
}
function hide_all() {
    $('.individual-details').hide();
}

function updateResultsWidth() {
    var description_width = 300;
    if (contentWidth < 600) {
        $('#results, #tree-header, #tree-footer').width(938);
        $('.individual-desc').width(description_width);
        $('.middle').width(537);
        $('#wrapper').width(938);
    } else {
        description_width = contentWidth - 500;
        $('#results, #tree-header, #tree-footer').width(contentWidth - 54);
        $('.individual-desc').width(description_width);
        $('.middle').width(contentWidth - 455);
        $('#wrapper').width(contentWidth - 54);
    }
}

$(function() {
    $('#selectLinks').buttonset();

    $(document).on('click', '.individual-header', function(e) {
        var link_id = $(this).parent().attr('id');
        if ($(e.target).is('a, img')) return;
        toggleLinkDetails(link_id);
    });

    var id;
    $(window).resize(function() {
        clearTimeout(id);
        id = setTimeout(updateResultsWidth, 100);
    });
});
