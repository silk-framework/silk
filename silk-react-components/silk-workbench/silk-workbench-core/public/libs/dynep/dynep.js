/* exported DynamicEndpointHandler */

/**
* Constructor for the DynamicEndpointHandler class.
* @class
* @classdesc <p>Utility class to handle adding, removing and painting dynamic endpoints
*   in <a href="https://jsplumbtoolkit.com">jsPlumb</a> projects.</p>
*   <p>For an element to have dynamic endpoints means that the number of endpoints
*   grows by one each time a new connection is made and shrinks by one if a connection
*   is removed. This means there is always one open, unconnected endpoint.</p>
*/
function DynamicEndpointHandler() {
    /**
  * @property {jsPlumbInstance} jsPlumbInstance - The object on which all jsPlumb 
  *   functions are called. 
  *   Default is the 
  *   <a href="https://jsplumbtoolkit.com/community/apidocs/classes/jsPlumb.html">
  *   jsPlumb</a> convenience class.
  */
    this.jsPlumbInstance = jsPlumb;

    var defaultStyle = this.jsPlumbInstance.Defaults.EndpointStyle;
    defaultStyle.isTarget = true;
    defaultStyle.isSource = false;
    defaultStyle.maxConnections = 1;

    this.styles = {};
    this.styles.default = defaultStyle;

    var connectionMovedToSameEndpoint = false;
    var _this = this;

    /**
  * Add a new dynamic endpoint to the DOM element with <code>elementId</code>.
  * @param elementId - The element to add the endpoint to.
  * @param type - The endpoint style. If <code>null</code>, the handler's <code>endpointStyle</code> will be used.
  */
    this.addDynamicEndpoint = function(elementId, type = 'default') {
        const style = this.styles[type];
        var newEndpoint = this.jsPlumbInstance.addEndpoint(elementId, style);
        newEndpoint.dynamic = true;
        newEndpoint.dynepType = type;

        this.repaintDynamicEndpoints(elementId);
        return newEndpoint;
    };

    /**
  * Remove <code>endpoint</code> from the DOM element with <code>elementId</code>.
  * @param elementId - The element to from which to remove the endpoint.
  * @param endpoint - The endpoint to remove.
  */
    this.removeDynamicEndpoint = function(elementId, endpoint) {
        this.jsPlumbInstance.deleteEndpoint(endpoint);
        this.repaintDynamicEndpoints(elementId);
    };

    /**
  * Repaint the endpoints of DOM element with <code>elementId</code> after
  *   adding or removing an endpoint.
  * @param elementId The element on which to repaint endpoints.
  */
    this.repaintDynamicEndpoints = function(elementId) {
        var endpoints = this.getDynamicEndpoints(elementId);
        this.repaintEndpoints(elementId, endpoints);
    };

    this.repaintEndpoints = function(elementId, endpoints) {
        var endpointCount = endpoints.length;

        // eslint-disable-next-line
        $.each(endpoints, function(index, value) {
            var position = 1 / (endpointCount + 1) * (index + 1);
            value.anchor.x = 0;
            value.anchor.y = position;
        });

        this.jsPlumbInstance.repaint(elementId);
    };

    this.getDynamicEndpoints = function(elementId) {
        var allEndpoints = this.jsPlumbInstance.getEndpoints(elementId);
        if (allEndpoints) {
            var dynamicEndpoints = $.grep(allEndpoints, function(endpoint) {
                return endpoint.dynamic;
            });
            return dynamicEndpoints;
        }
        return undefined;
    };

    this.getOpenDynamicEndpoint = function(elementId) {
        var endpoints = this.getDynamicEndpoints(elementId);
        return endpoints[endpoints.length - 1];
    };

    this.bindEvents = function() {
        this.jsPlumbInstance.bind('connection', function(info) {
            if (!connectionMovedToSameEndpoint) {
                if (info.targetEndpoint.dynamic) {
                    _this.addDynamicEndpoint(
                        info.targetId,
                        info.targetEndpoint.dynepType
                    );
                }
            } else {
                connectionMovedToSameEndpoint = false;
            }
        });

        this.jsPlumbInstance.bind('connectionDetached', function(info) {
            var targetEndpoint = info.targetEndpoint;
            if (targetEndpoint.dynamic) {
                setTimeout(function() {
                    // We need a little delay before removing the endpoint, as this will
                    // also remove any attached connections. If the detachment was already
                    // started elsewhere, we may get dangling pointers.
                    _this.removeDynamicEndpoint(info.targetId, targetEndpoint);
                }, 10);
            }
        });

        this.jsPlumbInstance.bind('connectionMoved', function(info) {
            // TODO: Check if info.originalTargetEndpoint, !== info.newTargetEndpoint
            console.warn(info.originalTargetEndpoint, info.newTargetEndpoint);
            // eslint-disable-next-line
            if (info.originalTargetEndpoint != info.newTargetEndpoint) {
                connectionMovedToSameEndpoint = false;
                var originalTargetEndpoint = info.originalTargetEndpoint;
                if (originalTargetEndpoint.dynamic) {
                    _this.removeDynamicEndpoint(
                        info.originalTargetId,
                        originalTargetEndpoint
                    );
                }
            } else {
                connectionMovedToSameEndpoint = true;
            }
        });

        this.jsPlumbInstance.bind('connectionAborted', function() {
            console.log('connection aborted ...');
        });
    };

    this.bindEvents();
}
