$(function() {
  // Initialize the jsQuery treeview plugin
  $(".entity-tree").treeview();
});

function expand_all() {
  $(".entity-content").show();
}

function hide_all() {
  $(".entity-content").hide();
}