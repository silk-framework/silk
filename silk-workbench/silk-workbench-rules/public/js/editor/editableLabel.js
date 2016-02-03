var defaultText = 'operator_id';

function endEdit(e) {
    var input = $(e.target),
        label = input && input.prev();

    if (e.which == 27) {
        // ESCAPE: reset value of input to text of label
        var labelText = label.text();
        input.val(labelText);
    } else {
        label.text(input.val() === '' ? defaultText : input.val());
    }
    input.hide();
    label.show();
}

// end editing by loosing focus
$( document ).on('blur', 'input.edit_label', function(e){
  endEdit(e);
});

// end editing by pressing return or escape
$( document ).on('keyup', 'input.edit_label', function(e) {
      if (e.which == 13 || e.which == 27) {
          // RETURN OR ESCAPE
          endEdit(e);
      }
});

// start editing
$( document ).on('click', 'label.edit_label', function(e) {
  $(this).hide();
  $(this).next().show().focus();
});