$(function() {
    // Initialize the jsQuery treeview plugin
    $('.entity-tree').treeview();
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
});

/* exported expand_all hide_all
silk-workbench/silk-workbench-rules/app/views/evaluateTransform/generatedEntities.scala.html
 */
function expand_all() {
    $('.entity-details').show();
}

function hide_all() {
    $('.entity-details').hide();
}
