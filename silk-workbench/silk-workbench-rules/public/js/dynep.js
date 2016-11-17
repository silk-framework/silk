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

  /**
  * @property endpointStyle - Default style of the dynamic endpoints. Default is
  *   <code>jsPlumbInstance.Defaults.EndpointStyle</code>, with these modifications:
  *   <pre>
  *      isTarget: true ,
  *      isSource: false ,
  *      maxConnections: 1</pre>
  */
  this.endpointStyle = this.jsPlumbInstance.Defaults.EndpointStyle;
  this.endpointStyle.isTarget = true;
  this.endpointStyle.isSource = false;
  this.endpointStyle.maxConnections = 1;

  var connectionMovedToSameEndpoint = false;
  var _this = this;

  /**
  * Add a new dynamic endpoint to the DOM element with <code>elementId</code>.
  * @param elementId - The element to add the endpoint to.
  * @param style - The endpoint style. If <code>null</code>, the handler's
  *   <code>endpointStyle</code> will be used.
  */
  this.addDynamicEndpoint = function (elementId, style=null) {
    console.log("addDynamicEndpoint: " + elementId);
    if (!style) {
      style = this.endpointStyle;
    }
    var newEndpoint = this.jsPlumbInstance.addEndpoint(elementId, style);
    $(newEndpoint.canvas).addClass("dynamic");
    var element = $("#" + elementId);
    if (!element.data("endpoints")) {
      element.data("endpoints", []);
    }
    element.data("endpoints").push(newEndpoint);

    this.repaintEndpoints(elementId);
  }

  /**
  * Remove <code>endpoint</code> from the DOM element with <code>elementId</code>.
  * @param elementId - The element to from which to remove the endpoint.
  * @param endpoint - The endpoint to remove.
  */
  this.removeDynamicEndpoint = function(elementId, endpoint) {
    this.jsPlumbInstance.deleteEndpoint(endpoint);
    var element = $("#" + elementId);
    var new_endpoints = $.grep(element.data("endpoints"), function(value) {
      return value != endpoint;
    });
    element.data("endpoints", new_endpoints);
    this.repaintEndpoints(elementId);
  }

  /**
  * Repaint the endpoints of DOM element with <code>elementId</code> after
  *   adding or removing an endpoint.
  * @param elementId The element on which to repaint endpoints.
  */
  this.repaintEndpoints = function(elementId) {
    var element = $("#" + elementId);
    var endpoints = element.data("endpoints");
    var endpointCount = endpoints.length;
    $.each(endpoints, function(index, value) {
      var position = 1 / (endpointCount + 1) * (index + 1) ;
      value.anchor.x = 0;
      value.anchor.y = position;
    });

    this.jsPlumbInstance.repaint(elementId);
  }

  this.jsPlumbInstance.bind("connection", function(info) {
    if (!connectionMovedToSameEndpoint) {
      var targetClasses = info.targetEndpoint.canvas.classList;
      if (targetClasses.contains("dynamic")) {
        _this.addDynamicEndpoint(info.targetId);
      }
    } else {
      connectionMovedToSameEndpoint = false;
    }
  });

  this.jsPlumbInstance.bind("connectionDetached", function(info) {
    var targetId = info.targetId;
    var targetClasses = info.targetEndpoint.canvas.classList;
    if (targetClasses.contains("dynamic")) {
      _this.removeDynamicEndpoint(targetId, info.targetEndpoint);
    }
  });

  this.jsPlumbInstance.bind("connectionMoved", function(info) {
    if (info.originalTargetEndpoint != info.newTargetEndpoint) {
      connectionMovedToSameEndpoint = false;
      var targetId = info.originalTargetId;
      var targetClasses = info.originalTargetEndpoint.canvas.classList;
      if (targetClasses.contains("dynamic")) {
        _this.removeDynamicEndpoint(targetId, info.originalTargetEndpoint);
      }
    } else {
      connectionMovedToSameEndpoint = true;
    }
  });

  this.jsPlumbInstance.bind("connectionAborted", function(connection, originalEvent) {
    console.log("connection aborted ...");
  });

}


