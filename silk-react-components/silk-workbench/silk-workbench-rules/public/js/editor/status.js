/* global highlightElement:true */

if (!highlightElement) {
    throw new Error('status.js must be imported as well as editor.js');
}

/* exported updateStatus showPendingIcon
silk-react-components/silk-workbench/silk-workbench-rules/public/js/editor/editor.js
*/

/**
 * Displays messages.
 * Each parameter contains an array of objects consisting of the following properties:
 * id (optional): The id of the corresponding rule element
 * message: The message to be displayed
 */
function updateStatus(messages) {

    const $tooltip =  $('#error-tooltip');

    $tooltip.html('');
    $tooltip.append(printMessages(messages));

    const errorCount = messages.filter(function(msg) {
        return msg.type === 'Error';
    }).length;

    if (errorCount > 0) {
        showInvalidIcon(errorCount);
        return;
    }

    const warningCount = messages.filter(function(msg) {
        return msg.type === 'Warning';
    }).length;

    if (warningCount > 0) {
        showWarningIcon(warningCount);
    } else {
        showValidIcon();
    }
}

function showValidIcon() {
    $('#exclamation, #warning').css('display', 'none');
    $('#tick').css('display', 'block');
    $('#pending').remove();
}

function showInvalidIcon(numberMessages) {
    const $excl = $('#exclamation');
    $('#tick, #warning').css('display', 'none');
    $('#pending').remove();
    $excl.attr('data-badge', numberMessages);
    $excl.css('display', 'block');
}

function showWarningIcon(numberMessages) {
    const $warn = $('#warning');
    $('#tick, #exclamation').css('display', 'none');
    $('#pending').remove();
    $warn.attr('data-badge', numberMessages);
    $warn.css('display', 'block');
}

/* exported updateStatus
silk-workbench/silk-workbench-rules/app/views/editor/status.scala.html
 */
function showPendingIcon() {
    $('#pending').remove();
    $('#exclamation, #warning, #tick').css('display', 'none');
    const $icons = $('#validation-icons');
    $icons.css('display', 'block');
    const $pending = document.createElement('div');
    $pending.id = 'pending';
    $pending.className = 'mdl-spinner mdl-spinner--single-color mdl-js-spinner is-active';
    componentHandler.upgradeElement($pending);
    $icons.append($pending)
}

function printMessages(array) {
    let result = '';
    for (let i = 0; i < array.length; i++) {
        result += `<div class="msg">${i + 1}. ${encodeHtml(array[i].message)}</div>`;
        if (array[i].id){
            highlightElement(array[i].id, encodeHtml(array[i].message));
        }
    }
    return result;
}

function encodeHtml(value) {
    return _.escape(value);
}
