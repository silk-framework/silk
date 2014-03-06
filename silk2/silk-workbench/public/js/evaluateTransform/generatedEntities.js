$(function() {
  // Initialize the jsQuery treeview plugin
  $(".entity-tree").treeview();
});

function expand_all() {
  $(".entity-details").show();
}

function hide_all() {
  $(".entity-details").hide();
}