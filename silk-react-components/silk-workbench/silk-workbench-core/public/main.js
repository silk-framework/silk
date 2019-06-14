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

/* global dialogPolyfill:true, componentHandler: true */

/**
 * Global JavaScript functions.
 */

/* exported contentWidth
silk-react-components/silk-workbench/silk-workbench-rules/public/js/links.js
silk-react-components/silk-workbench/silk-workbench-rules/public/js/population.js
*/
var helpWidth = 170;
var contentWidth;
var contentWidthCallback = function() {};
// The currently open dialog
var primary_dialog;
var secondary_dialog;
var dialogs = {};

let dialog;

const startTime = new Date().getTime();
let now = new Date().getTime();

window.timeDump = function(str) {
    const temp = new Date().getTime();
    console.warn(str, temp - now, temp - startTime);
    now = temp;
};

$(function() {
    // Make sure that mdl components are registered the right way
    componentHandler.upgradeDom();

    // Initialize window
    var resize = _.throttle(function() {
        contentWidth = $(window).width() - helpWidth;
        contentWidthCallback();
    }, 100);

    $(window).on('resize', resize);

    contentWidth = $(window).width() - 190;
    contentWidthCallback();

    // Initialize dialog
    primary_dialog = document.querySelector('#primary_dialog');
    if (primary_dialog) {
        dialogs.primary = primary_dialog;
    }
    secondary_dialog = document.querySelector('#secondary_dialog');
    if (secondary_dialog) {
        dialogs.secondary = secondary_dialog;
    }
});

var errorHandler = function(request) {
    if (request.responseText) {
        alert(request.responseText);
    } else {
        alert(request.statusText);
    }
};

/**
 * Opens a dialog.
 */
function showDialog(path, dialog_key = 'primary', payload = {}) {
    dialog = dialogs[dialog_key];
    $.data(dialog, 'path', path);
    $.get(path, payload, function(data) {
        // inject dialog content into dialog container
        $(dialog).html(data);
        // enable MDL JS for dynamically added components
        componentHandler.upgradeAllRegistered();
    })
    .done(function() {
        if (!dialog.showModal) {
            dialogPolyfill.registerDialog(dialog);
        }
        dialog.showModal();
    })
    .fail(function(request) {
        alert(request.responseText);
    });
}

/**
 * Reloads a dialog.
 */
function reloadDialog(dialog_key = 'primary') {
    dialog = dialogs[dialog_key];
    var path = $.data(dialog, 'path');
    $.get(path, function(data) {
        $(dialog).html(data);
        componentHandler.upgradeAllRegistered();
    }).fail(function(request) {
        alert(request.responseText);
    });
}

/**
 * Closes current dialog.
 */
function closeDialog(dialog_key = 'primary') {
    dialog = dialogs[dialog_key];
    dialog.close();
}

// TODO Apparently unused?
/* exported showHelp, hideHelp */

/**
 * Shows the help sidebar.
 */
function showHelp() {
    updateHelpWidth(170);
    $('#show-help').hide();
    $('#help').show('slide', {direction: 'right'}, 'slow');
}

/**
 * Hides the help sidebar.
 */
function hideHelp() {
    $('#help').hide('slide', {direction: 'right'}, 'slow', function() {
        updateHelpWidth(16);
        $('#show-help').show();
    });
}

function updateHelpWidth(newWidth) {
    helpWidth = newWidth;
    contentWidth = $(window).width() - helpWidth;
    contentWidthCallback();
}

/*
 *
 * With the following code, we throttle fired mousemove events by jquery UI to 50 FPS
 *
 */

let jqueryDragActive = false;

const originalMouseMove = jQuery.ui.mouse.prototype._mouseMove;
jQuery.ui.mouse.prototype._mouseMove = function() {
    if (jqueryDragActive) {
        originalMouseMove.apply(this, arguments);
    }
};

const originalMouseDown = jQuery.ui.mouse.prototype._mouseDown;
jQuery.ui.mouse.prototype._mouseDown = function() {
    jqueryDragActive = true;
    originalMouseDown.apply(this, arguments);
};

const originalMouseUp = jQuery.ui.mouse.prototype._mouseUp;

jQuery.ui.mouse.prototype._mouseUp = function() {
    originalMouseUp.apply(this, arguments);
    jqueryDragActive = false;
};

jQuery.ui.mouse.prototype._mouseMove = _.throttle(
    jQuery.ui.mouse.prototype._mouseMove,
    20
);
/**
 * This functions searches for all visible deferred mdl elements and upgrades them accordingly
 * @param $parent
 */
function activateDeferredMDL($parent) {
    const deferredMDL = $parent.find('.mdl-defer');

    deferredMDL.filter(':visible').each(function() {
        const $elem = $(this);
        $elem.removeClass('mdl-defer');
        const deferred = $elem.data('mdl-defer');
        $elem.addClass(`mdl-${deferred}`);
        componentHandler.upgradeElements($elem.get());
    });
}

function generateNewIdsForTooltips($parent) {
    $parent.find('.mdl-defer').each(function() {
        const $elem = $(this);
        const forAttr = $elem.attr('for');
        if (forAttr) {
            const $target = $parent.find(`#${forAttr}`);
            if (_.size($target) > 0) {
                const newID = _.uniqueId(forAttr);
                $target.attr('id', newID);
                $elem.attr('for', newID);
            }
        }
    });
}

/**
 * Connects to a WebSocket on window load and calls a provided function on every update.
 *
 * param webSocketUrl Address of a WebSocket endpoint that supplies updates via JSON objects.
 * param pollingUrl Address of a polling endpoint for fallback.
 * param updateFunc Function to be called on every update. Receives a single argument, which is the received JSON object.
 */
function connectWebSocket(webSocketUrl, pollingUrl, updateFunc) {
    const websocket = new WebSocket(webSocketUrl);
    websocket.onmessage = function(evt) { updateFunc(JSON.parse(evt.data)) };
    websocket.onerror = function(evt) {
        console.log("Connecting to WebSocket at '" + webSocketUrl + "' failed. Falling back to polling...");
        let lastUpdate = 0;
        setInterval(function() {
            let currentTime = new Date().getTime();
            let timestampedUrl = pollingUrl + (pollingUrl.indexOf('?') >= 0 ? "&" : '?') + lastUpdate;
            $.getJSON(timestampedUrl, function(data) {
                lastUpdate = currentTime;
                if(Array.isArray(data)) {
                    data.forEach(updateFunc)
                } else {
                    updateFunc(data);
                }
            });
        }, 500);
    };
}
