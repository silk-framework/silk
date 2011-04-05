var aggregatecounter = 0;
var transformcounter = 0;
var comparecounter = 0;
var sourcecounter = 0;
var targetcounter = 0;

var transformations = new Object();
var comparators = new Object();
var aggregators = new Object();

var sources = new Array();
var targets = new Array();

var interlinkId = "";

var sourceDataSet = "";
var targetDataSet = "";
var sourceDataSetVar = "";
var targetDataSetVar = "";
var sourceDataSetRestriction = "";
var targetDataSetRestriction = "";

var endpointOptions =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: true,
  style: {
    fillStyle: '#890685'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#890685'],
        [1, '#359ace']
      ]
    },
    strokeStyle: '#890685',
    lineWidth: 5
  },
  isTarget: false,
  anchor: "RightMiddle"
};

var endpointOptions1 =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: false,
  style: {
    fillStyle: '#359ace'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#359ace'],
        [1, '#35ceb7']
      ]
    },
    strokeStyle: '#359ace',
    lineWidth: 5
  },
  isTarget: true,
  maxConnections: 4,
  anchor: "LeftMiddle"
};

var endpointOptions2 =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: true,
  style: {
    fillStyle: '#35ceb7'
  },
  connectorStyle: {
    gradient: {
      stops: [
        [0, '#35ceb7'],
        [1, '#359ace']
      ]
    },
    strokeStyle: '#359ace',
    lineWidth: 5
  },
  isTarget: true,
  maxConnections: 1,
  anchor: "RightMiddle"
};

document.onselectstart = function ()
{
  return false;
};

Array.max = function(array) {
    return Math.max.apply(Math, array);
};

function findLongestPath(xml)
{
  if ($(xml).children().length > 0)
  {
    var xmlHeight = [];
    var i = 0;
    $(xml).children().each(function()
    {
      xmlHeight[i] = findLongestPath($(this));
      i = i+1;
    });
    maxLength = Math.max.apply(null, xmlHeight);
    return 1 + maxLength;
  }
  else
  {
    return 0;
  }
}

function getHelpIcon(description, marginTop) {
  var helpIcon = $(document.createElement('img'));
  helpIcon.attr("src", "static/img/help.png");
  if ((marginTop == null) || (marginTop > 0)) {
    helpIcon.attr("style", "margin-top: 6px; cursor:help;");
  } else {
    helpIcon.attr("style", "margin-bottom: 3px; cursor:help;");
  }
  helpIcon.attr("align", "right");
  helpIcon.attr("title", description);
  return helpIcon;
}

function getDeleteIcon(elementId) {
  var img = $(document.createElement('img'));
  img.attr("src", "static/img/delete.png");
  img.attr("align", "right");
  img.attr("style", "cursor:pointer;");
  img.attr("onclick", "jsPlumb.removeAllEndpoints('" + elementId+"');$('" + elementId+"').remove();");
  return img;
}

function parseXML(xml, level, level_y, last_element, max_level, lastElementId)
{
  $(xml).find("> Aggregate").each(function ()
  {
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv aggregateDiv');
    box1.attr("id", "aggregate_" + aggregatecounter);

    var height = aggregatecounter * 120 + 120;
    var left = (max_level*250) - ((level + 1) * 250) + 260;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#aggregate_" + aggregatecounter;
    box1.draggable(
    {
      containment: '#droppable'
    });
    box1.appendTo("#droppable");

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("type"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Aggregate");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", aggregators[$(this).attr("type")]["name"] + " (Aggregator)")
    var mytext = document.createTextNode(aggregators[$(this).attr("type")]["name"] + " (Aggregator)");
    span.append(mytext);
    box2.append(span);
	
    box2.append(getDeleteIcon("#aggregate_" + aggregatecounter));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
    box3.attr("name", "required");
    box3.attr("type", "checkbox");
    if ($(this).attr("required") == "true")
    {
      box3.attr("checked", "checked");
    }
    box2.append(box3);

    var box4 = $(document.createElement('br'));
    box2.append(box4);

    var mytext = document.createTextNode("weight: ");
    box2.append(mytext);

    var box5 = $(document.createElement('input'));
    box5.attr("type", "text");
    box5.attr("name", "weight");
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    $params = Object();
    $(this).find("> Param").each(function ()
    {
      $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(aggregators[$(this).attr("type")]["parameters"], function(j, parameter) {
      var box4 = $(document.createElement('br'));
      box2.append(box4);

      var mytext = document.createTextNode(parameter.name + ": ");
      box2.append(mytext);

      var box5 = $(document.createElement('input'));
      box5.attr("type", "text");
      box5.attr("name", parameter.name);
      box5.attr("size", "10");
      if ($params[parameter.name]) {
        box5.attr("value", $params[parameter.name]);
      }
      box2.append(box5);
    });

    box2.append(getHelpIcon(aggregators[$(this).attr("type")]["description"]));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions2);
    aggregatecounter = aggregatecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));

  });
  $(xml).find("> Compare").each(function ()
  {
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv compareDiv');
    box1.attr("id", "compare_" + comparecounter);

    var height = comparecounter * 120 + 120;
    var left = (max_level*250) - ((level + 1) * 250) + 260;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#compare_" + comparecounter;
    box1.draggable(
    {
      containment: '#droppable'
    });
    box1.appendTo("#droppable");

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("metric"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Compare");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", comparators[$(this).attr("metric")]["name"] + " (Comparator)")
    var mytext = document.createTextNode(comparators[$(this).attr("metric")]["name"] + " (Comparator)");
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon("#compare_" + comparecounter));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
    box3.attr("name", "required");
    box3.attr("type", "checkbox");
    if ($(this).attr("required") == "true")
    {
      box3.attr("checked", "checked");
    }
    box2.append(box3);

    var box4 = $(document.createElement('br'));
    box2.append(box4);

    var mytext = document.createTextNode("weight: ");
    box2.append(mytext);

    var box5 = $(document.createElement('input'));
    box5.attr("type", "text");
    box5.attr("name", "weight");
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    $params = Object();
    $(this).find("> Param").each(function ()
    {
        $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(comparators[$(this).attr("metric")]["parameters"], function(j, parameter) {
        var box4 = $(document.createElement('br'));
        box2.append(box4);

        var mytext = document.createTextNode(parameter.name + ": ");
        box2.append(mytext);

        var box5 = $(document.createElement('input'));
        box5.attr("type", "text");
        box5.attr("name", parameter.name);
        box5.attr("size", "10");
        if ($params[parameter.name]) {
          box5.attr("value", $params[parameter.name]);
        }
        box2.append(box5);
    });

    box2.append(getHelpIcon(comparators[$(this).attr("metric")]["description"]));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions2);
    comparecounter = comparecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));
  });

  $(xml).find("> TransformInput").each(function ()
  {
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv transformDiv');
    box1.attr("id", "transform_" + transformcounter);

    var height = transformcounter * 120 + 120;
    var left = (max_level*250) - ((level + 1) * 250) + 260;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#transform_" + transformcounter;
    box1.draggable(
    {
      containment: '#droppable'
    });
    box1.appendTo("#droppable");

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode($(this).attr("function"));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("TransformInput");
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    span.attr("title", transformations[$(this).attr("function")]["name"] + " (Transformation)")
    var mytext = document.createTextNode(transformations[$(this).attr("function")]["name"] + " (Transformation)");
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon("#transform_" + transformcounter));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    $params = Object();
    $(this).find("> Param").each(function ()
    {
        $params[$(this).attr("name")] = $(this).attr("value");
    });

    $.each(transformations[$(this).attr("function")]["parameters"], function(j, parameter) {
        if (j > 0) {
            var box4 = $(document.createElement('br'));
        }
        box2.append(box4);

        var mytext = document.createTextNode(parameter.name + ": ");
        box2.append(mytext);

        var box5 = $(document.createElement('input'));
        box5.attr("type", "text");
        box5.attr("name", parameter.name);
        box5.attr("size", "10");
        if ($params[parameter.name]) {
            box5.attr("value", $params[parameter.name]);
        }
        box2.append(box5);
    });

    box2.append(getHelpIcon(transformations[$(this).attr("function")]["description"], transformations[$(this).attr("function")]["parameters"].length));

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions2);
    transformcounter = transformcounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    parseXML($(this), level + 1, 0, endp_left, max_level, box1.attr("id"));
  });
  $(xml).find("> Input").each(function ()
  {

    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv sourcePath');
    box1.attr("id", "source_" + sourcecounter);

    var height = sourcecounter * 120 + 120;
    var left = (max_level*250) - ((level + 1) * 250) + 260;
    box1.attr("style", "left: " + left + "px; top: " + height + "px; position: absolute;");

    var number = "#source_" + sourcecounter;
    box1.draggable(
    {
      containment: '#droppable'
    });
    box1.appendTo("#droppable");

    var box2 = $(document.createElement('small'));
    box2.addClass('name');
    var mytext = document.createTextNode(encodeHtml($(this).attr("path")));
    box2.append(mytext);
    box1.append(box2);
    var box2 = $(document.createElement('small'));
    box2.addClass('type');
    var mytext = document.createTextNode("Input");
    box2.append(mytext);
    box1.append(box2);
	
    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');

    var span = $(document.createElement('div'));
    span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
    // TODO
    /*
    if (($(this).attr("path")).indexOf("\\") > 0) {
      alert($(this).attr("path"));
    }
    */
    span.attr("title", encodeHtmlInput($(this).attr("path")));
    var mytext = document.createTextNode(encodeHtmlInput($(this).attr("path")));
    span.append(mytext);
    box2.append(span);

    box2.append(getDeleteIcon("#source_" + sourcecounter));

    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    box1.append(box2);

    var endp_right = jsPlumb.addEndpoint('source_' + sourcecounter, endpointOptions);
    sourcecounter = sourcecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: endp_right,
        targetEndpoint: last_element
      });
    }
    parseXML($(this), level + 1, 0, endp_right, max_level, box1.attr("id"));
  });
}

function load()
{
  //alert(linkSpec);
  interlinkId = $(linkSpec).attr("id");
  
  $(linkSpec).find("> SourceDataset").each(function ()
  {
    sourceDataSet = $(this).attr("dataSource");
    sourceDataSetVar = $(this).attr("var");
    $(this).find("> RestrictTo").each(function ()
    {
      sourceDataSetRestriction = $(this).text();
    });
  });
  $(linkSpec).find("> TargetDataset").each(function ()
  {
    targetDataSet = $(this).attr("dataSource");
    targetDataSetVar = $(this).attr("var");
    $(this).find("> RestrictTo").each(function ()
    {
      targetDataSetRestriction = $(this).text();
    });
  });
  
  $(linkSpec).find("> LinkCondition").each(function ()
  {
    var max_level = findLongestPath($(this));

    /*
    var userAgent = navigator.userAgent.toLowerCase();
    // Figure out what browser is being used
    jQuery.browser = {
      version: (userAgent.match( /.+(?:rv|it|ra|ie|me)[\/: ]([\d.]+)/ ) || [])[1],
      chrome: /chrome/.test( userAgent ),
      safari: /webkit/.test( userAgent ) && !/chrome/.test( userAgent ),
      opera: /opera/.test( userAgent ),
      msie: /msie/.test( userAgent ) && !/opera/.test( userAgent ),
      mozilla: /mozilla/.test( userAgent ) && !/(compatible|webkit)/.test( userAgent )
    };
    var is_chrome = /chrome/.test( navigator.userAgent.toLowerCase());
    var is_safari = /safari/.test( navigator.userAgent.toLowerCase());
    */

    parseXML($(this), 0, 0, "", max_level, "");
    if ((sourcecounter*120 + 20) > 800) {
       $("#droppable").css( { "height": (sourcecounter*120 + 20) + "px" });
    }
  });
  $(linkSpec).find("> LinkType").each(function ()
  {
    $("#linktype").attr("value", $(this).text());
  });
  $(linkSpec).find("> Filter").each(function ()
  {
    if ($(this).attr("limit") > 0) {
      $("select[id=linklimit] option[text="+$(this).attr("limit")+"]").attr("selected", true);
    }
    $("#threshold").attr("value", $(this).attr("threshold"));
  });
  updateWindowWidth();
}


function updateWindowWidth() {
  var window_width =  $(window).width();
  if (window_width>1100) {
    $(".wrapper").width(window_width-10);
    $("#droppable").width(window_width-290);
  } else {
    $(".wrapper").width(1000+1200-window_width);
    $("#droppable").width(830);
  }
}

function getHTML(who, deep)
{
  if (!who || !who.tagName) return '';
  var txt, el = document.createElement("div");
  el.appendChild(who.cloneNode(deep));
  txt = el.innerHTML;
  el = null;
  return txt;
}

function createNewElement(elementId)
{
	var elementIdName = "#"+elementId;
	var elName = ($(elementIdName).children(".name").text());
	var elType = ($(elementIdName).children(".type").text());
	var xml = document.createElement(elType);
	if (elType == "Input") {
    if (elName == "") {
      xml.setAttribute("path", $(elementIdName+" > h5 > input").val());
    } else {
      xml.setAttribute("path", decodeHtml(elName));
    }
	} else if (elType == "TransformInput") {
		xml.setAttribute("function", elName);
    } else if (elType == "Aggregate") {
		xml.setAttribute("type", elName);
	} else if (elType == "Compare") {
		xml.setAttribute("metric", elName);
	}
  var params = $(elementIdName+" > div.content > input");

	var c = jsPlumb.getConnections();
	for (var i = 0; i < c[jsPlumb.getDefaultScope()].length; i++)
    {
        var source = c[jsPlumb.getDefaultScope()][i].sourceId;
        var target = c[jsPlumb.getDefaultScope()][i].targetId;
        if (target == elementId)
        {
          xml.appendChild(createNewElement(source));
        }
    }

    for (var l = 0; l < params.length; l++) {
      if ($(params[l]).attr("name") == "required") {
        if (($(elementIdName+" > div.content > input[name=required]:checked").val()) == "on") {
            xml.setAttribute("required", "true");
        } else {
            xml.setAttribute("required", "false");
        }
      } else if ($(params[l]).attr("name") == "weight") {
        xml.setAttribute("weight", $(params[l]).attr("value"));
      } else {
        if (elType == "Compare") {
          if ($(params[l]).val() != "") {
            var xml_param = document.createElement("Param");
            xml_param.setAttribute("name", $(params[l]).attr("name"));
            xml_param.setAttribute("value", $(params[l]).val());
            xml.appendChild(xml_param);
          }
        } else {
          var xml_param = document.createElement("Param");
          xml_param.setAttribute("name", $(params[l]).attr("name"));
          xml_param.setAttribute("value", $(params[l]).val());
          xml.appendChild(xml_param);
        }
      }
    }

    return xml;
}

function serializeLinkSpec() {
  //alert (JSON.stringify(c));

  var c = jsPlumb.getConnections();
  if (c[jsPlumb.getDefaultScope()] !== undefined) {
    var connections = "";
    for (var i = 0; i < c[jsPlumb.getDefaultScope()].length; i++)
    {
      var source = c[jsPlumb.getDefaultScope()][i].sourceId;
      var target = c[jsPlumb.getDefaultScope()][i].targetId;
      sources[target] = source;
      targets[source] = target;
      connections = connections + source + " -> " + target + ", ";
    }
    //alert (connections);
    var root = null;
    for (var key in sources)
    {
      if (!targets[key])
      {
        root = key;
      }
    }
  }
  // alert(connections + "\n\n" + root);
  var xml = document.createElement("Interlink");
  xml.setAttribute("id", interlinkId);

  var linktype = document.createElement("LinkType");
  var linktypeText = document.createTextNode($("#linktype").val());
  linktype.appendChild(linktypeText);
  xml.appendChild(linktype);

  var sourceDataset = document.createElement("SourceDataset");
  sourceDataset.setAttribute("var", sourceDataSetVar);
  sourceDataset.setAttribute("dataSource", sourceDataSet);
  var restriction = document.createElement("RestrictTo");
  var restrictionText = document.createTextNode(sourceDataSetRestriction);
  restriction.appendChild(restrictionText);
  sourceDataset.appendChild(restriction);
  xml.appendChild(sourceDataset);

  var targetDataset = document.createElement("TargetDataset");
  targetDataset.setAttribute("var", targetDataSetVar);
  targetDataset.setAttribute("dataSource", targetDataSet);
  var restriction = document.createElement("RestrictTo");
  var restrictionText = document.createTextNode(targetDataSetRestriction);
  restriction.appendChild(restrictionText);
  targetDataset.appendChild(restriction);
  xml.appendChild(targetDataset);

  var linkcondition = document.createElement("LinkCondition");
  if (root != null)
  {
    linkcondition.appendChild(createNewElement(root));
  }
  xml.appendChild(linkcondition);

  var filter = document.createElement("Filter");
  if ($("#linklimit :selected").text() != "unlimited")
  {
    filter.setAttribute("limit", $("#linklimit :selected").text());
  }
  filter.setAttribute("threshold", $("#threshold").val());
  xml.appendChild(filter);

  var outputs = document.createElement("Outputs");
  xml.appendChild(outputs);

  var xmlString = getHTML(xml, true);
  xmlString = xmlString.replace('xmlns="http://www.w3.org/1999/xhtml"', "");
  // alert(xmlString);
  return xmlString;
}

$(function ()
{
  $("#droppable").droppable(
  //{ tolerance: 'touch' },
  {
    drop: function (ev, ui)
    {
      if ($("#droppable").find("> #"+ui.helper.attr('id')+"").length == 0) {
        //$(this).append($(ui.helper).clone());
        $.ui.ddmanager.current.cancelHelperRemoval = true;
        ui.helper.appendTo(this);

        /*
        styleString = $("#"+ui.helper.attr('id')).attr("style");
        styleString = styleString.replace("position: absolute", "");
        $("#"+ui.helper.attr('id')).attr("style", styleString);
        */
        if (ui.helper.attr('id').search(/aggregate/) != -1)
        {
          jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions1);
          jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions2);
          var number = "#aggregate_" + aggregatecounter;
          $(number).draggable(
          {
            containment: '#droppable',
            drag: function(event, ui) {
              jsPlumb.repaint(number);
            }
          });
          aggregatecounter = aggregatecounter + 1;
        }
        if (ui.helper.attr('id').search(/transform/) != -1)
        {
          jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions1);
          jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions2);
          var number = "#transform_" + transformcounter;
          $(number).draggable(
          {
            containment: '#droppable',
            drag: function(event, ui) {
              jsPlumb.repaint(number);
            }
          });
          transformcounter = transformcounter + 1;
        }
        if (ui.helper.attr('id').search(/compare/) != -1)
        {
          jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions1);
          jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions2);
          var number = "#compare_" + comparecounter;
          $(number).draggable(
          {
            containment: '#droppable',
            drag: function(event, ui) {
              jsPlumb.repaint(number);
            }
          });
          comparecounter = comparecounter + 1;
        }
        if (ui.helper.attr('id').search(/source/) != -1)
        {
          jsPlumb.addEndpoint('source_' + sourcecounter, endpointOptions);
          var number = "#source_" + sourcecounter;
          $(number).draggable(
          {
            containment: '#droppable',
            drag: function(event, ui) {
              jsPlumb.repaint(number);
            }
          });
          sourcecounter = sourcecounter + 1;
        }
        if (ui.helper.attr('id').search(/target/) != -1)
        {
          jsPlumb.addEndpoint('target_' + targetcounter, endpointOptions);
          var number = "#target_" + targetcounter;
          $(number).draggable(
          {
            containment: '#droppable',
            drag: function(event, ui) {
              jsPlumb.repaint(number);
            }
          });
          targetcounter = targetcounter + 1;
        }
      }
    }
  });
});

function decodeHtml(value)
{
  encodedHtml = value.replace("&lt;", "<");
  encodedHtml = encodedHtml.replace("&gt;", ">");
  return encodedHtml;
}

function encodeHtml(value)
{
  encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}

function encodeHtmlInput(value)
{
  var encodedHtml = value.replace('\\', "&#92;");
  return encodedHtml;
}

function getPropertyPaths(deleteExisting)
{
  if (deleteExisting)
  {
    $("#paths").empty();
    var box = $(document.createElement('div'));
    box.attr("id", "loading");
    box.attr("style", "width: 230px;");
    var text = document.createTextNode("loading ...");
    box.append(text);
    box.appendTo("#paths");
  }
  var url = "api/project/paths"; // ?max=10
  $.getJSON(url, function (data)
  {
    if(data.isLoading)
    {
	    var dot = document.createTextNode(".");
      document.getElementById("loading").appendChild(dot);
      setTimeout("getPropertyPaths();", 1000);
    }
    else if (data.error !== undefined)
    {
      alert("Could not load property paths.\nError: " + data.error);
    }
    else
    {
      document.getElementById("paths").removeChild(document.getElementById("loading"));

    var list_item_id = 0;

    var box = $(document.createElement('div'));
    box.html("<span style='font-weight: bold;'>Source:</span> " + data.source.id).appendTo("#paths");
    box.appendTo("#paths");

    var box = $(document.createElement('div'));
    box.addClass('more');
    box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.source.restrictions).appendTo("#paths");
    box.appendTo("#paths");

    var box = $(document.createElement('div'));
    box.attr("id", "sourcepaths");
    box.addClass("scrollboxes");
    box.appendTo("#paths");

    var sourcepaths = data.source.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable');
      box.attr("id", "source" + list_item_id);
      box.attr("title", encodeHtml(item.path));
      box.html("<span></span><p style=\"white-space:nowrap; overflow:hidden;\">" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv sourcePath');
          box1.attr("id", "source_" + sourcecounter);

          var box2 = $(document.createElement('small'));
          box2.addClass('name');
          var mytext = document.createTextNode(encodeHtml(item.path));
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('small'));
          box2.addClass('type');
          var mytext = document.createTextNode("Input");
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('h5'));
          box2.addClass('handler');

          var span = $(document.createElement('div'));
          span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
          span.attr("title", item.path);
          var mytext = document.createTextNode(item.path);
          span.append(mytext);
          box2.append(span);

          box2.append(getDeleteIcon("#source_" + sourcecounter));

          box1.append(box2);

          var box2 = $(document.createElement('div'));
          box2.addClass('content');
          box1.append(box2);

          return box1;
        }
      });
      box.appendTo("#sourcepaths");
      list_item_id = list_item_id + 1;

    });

    var box = $(document.createElement('div'));
    box.addClass('draggable');
    box.attr("id", "source" + list_item_id);
    box.html("<span> </span><small> </small><p>(custom path)</p>");
    box.draggable(
    {
      helper: function ()
      {
        var box1 = $(document.createElement('div'));
        box1.addClass('dragDiv sourcePath');
        box1.attr("id", "source_" + sourcecounter);

        var box2 = $(document.createElement('small'));
        box2.addClass('name');
        box1.append(box2);

        var box2 = $(document.createElement('small'));
        box2.addClass('type');
        var mytext = document.createTextNode("Input");
        box2.append(mytext);
        box1.append(box2);

        var box2 = $(document.createElement('h5'));
        box2.addClass('handler');
        box2.attr("style", "height: 19px;");

        var input = $(document.createElement('input'));
        input.attr("style", "width: 165px;");
        input.attr("type", "text");
        input.val("?" + sourceDataSetVar);
        box2.append(input);

        box2.append(getDeleteIcon("#source_" + sourcecounter));

        box1.append(box2);

        var box2 = $(document.createElement('div'));
        box2.addClass('content');
        box1.append(box2);

        return box1;
      }
    });
    box.appendTo("#sourcepaths");

    /*
    var availablePaths = data.source.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html("<a href='/linkSpec' class='more'>&darr; more source paths...</a>");
      box.appendTo("#paths");
    }
    */

    var box = $(document.createElement('div'));
    box.html("<span style='font-weight: bold;'>Target:</span> " + data.target.id).appendTo("#paths");
    box.appendTo("#paths");

    var box = $(document.createElement('div'));
    box.addClass('more');
    box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.target.restrictions).appendTo("#paths");
    box.appendTo("#paths");

    var list_item_id = 0;

    var box = $(document.createElement('div'));
    box.attr("id", "targetpaths");
    box.addClass("scrollboxes");
    box.appendTo("#paths");

    var sourcepaths = data.target.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable');
      box.attr("id", "target" + list_item_id);
      box.attr("title", encodeHtml(item.path));
      box.html("<span></span><small>" + encodeHtml(item.path) + "</small><p style=\"white-space:nowrap; overflow:hidden;\">" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv targetPath');
          box1.attr("id", "target_" + targetcounter);

          var box2 = $(document.createElement('small'));
          box2.addClass('name');
          var mytext = document.createTextNode(encodeHtml(item.path));
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('small'));
          box2.addClass('type');
          var mytext = document.createTextNode("Input");
          box2.append(mytext);
          box1.append(box2);

          var box2 = $(document.createElement('h5'));
          box2.addClass('handler');

          var span = $(document.createElement('div'));
          span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
          span.attr("title", item.path);
          var mytext = document.createTextNode(item.path);
          span.append(mytext);
          box2.append(span);

          box2.append(getDeleteIcon("#target_" + targetcounter));

          box1.append(box2);

          var box2 = $(document.createElement('div'));
          box2.addClass('content');
          box1.append(box2);

          return box1;
        }
      });
      box.appendTo("#targetpaths");

      list_item_id = list_item_id + 1;

    });

    var box = $(document.createElement('div'));
    box.addClass('draggable');
    box.attr("id", "target" + list_item_id);
    box.html("<span> </span><small> </small><p>(custom path)</p>");
    box.draggable(
    {
      helper: function ()
      {
        var box1 = $(document.createElement('div'));
        box1.addClass('dragDiv sourcePath');
        box1.attr("id", "source_" + sourcecounter);

        var box2 = $(document.createElement('small'));
        box2.addClass('name');
        box1.append(box2);

        var box2 = $(document.createElement('small'));
        box2.addClass('type');
        var mytext = document.createTextNode("Input");
        box2.append(mytext);
        box1.append(box2);

        var box2 = $(document.createElement('h5'));
        box2.addClass('handler');
        box2.attr("style", "height: 19px;");

        var input = $(document.createElement('input'));
        input.attr("style", "width: 165px;");
        input.attr("type", "text");
        input.val("?" + targetDataSetVar);
        box2.append(input);

        box2.append(getDeleteIcon("#source_" + sourcecounter));

        box1.append(box2);

        var box2 = $(document.createElement('div'));
        box2.addClass('content');
        box1.append(box2);

        return box1;
      }
    });
    box.appendTo("#targetpaths");
    /*
    var availablePaths = data.target.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html('<a href="" class="more">&darr; more target paths... (' + availablePaths + ' in total)</a>');
      box.appendTo("#paths");

    }
    */
    }
  });
}

function getOperators()
{

  $.ajax(
  {
    type: "GET",
    url: "api/project/operators",
    contentType: "application/json; charset=utf-8",
    dataType: "json",
    timeout: 2000,
    success: function (data, textStatus, XMLHttpRequest)
    {
      // alert("success: " + data + " " + textStatus + " " + XMLHttpRequest.status);
      if (XMLHttpRequest.status >= 200 && XMLHttpRequest.status < 300)
      {
        var list_item_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #0cc481;");
        box.addClass("boxheaders");
        box.html("Transformations").appendTo("#operators");
        box.appendTo("#operators");

        var box = $(document.createElement('div'));
        box.attr("id", "transformationbox");
        box.addClass("scrollboxes");
        box.appendTo("#operators");

        var sourcepaths = data.transformations;
        $.each(sourcepaths, function (i, item)
        {
          transformations[item.id] = new Object();
          transformations[item.id]["name"] = item.label;
          transformations[item.id]["description"] = item.description;
          transformations[item.id]["parameters"] = item.parameters;

          var box = $(document.createElement('div'));
          box.addClass('draggable tranformations');
          box.attr("id", "transformation" + list_item_id);
          box.attr("title", item.description);
          box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");
          box.draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv transformDiv');
              box1.attr("id", "transform_" + transformcounter);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("TransformInput");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Transformation)")
              var mytext = document.createTextNode(item.label + " (Transformation)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon("#transform_" + transformcounter));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              $.each(item.parameters, function (j, parameter)
              {
                if (j > 0)
                {
                  var box4 = $(document.createElement('br'));
                  box2.append(box4);
                }

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description, item.parameters.length));

              box1.append(box2);
              return box1;
            }
          });
          box.appendTo("#transformationbox");

          list_item_id = list_item_id + 1;
        });

        var list_item_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #e59829;");
        box.addClass("boxheaders");
        box.html("Comparators").appendTo("#operators");
        box.appendTo("#operators");

        var box = $(document.createElement('div'));
        box.attr("id", "comparatorbox");
        box.addClass("scrollboxes");
        box.appendTo("#operators");

        var sourcepaths = data.comparators;
        $.each(sourcepaths, function (i, item)
        {
          comparators[item.id] = new Object();
          comparators[item.id]["name"] = item.label;
          comparators[item.id]["description"] = item.description;
          comparators[item.id]["parameters"] = item.parameters;
          var box = $(document.createElement('div'));
          box.addClass('draggable comparators');
          box.attr("id", "comparator" + list_item_id);
          box.attr("title", item.description);
          box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");
          box.draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv compareDiv');
              box1.attr("id", "compare_" + comparecounter);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("Compare");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Comparator)")
              var mytext = document.createTextNode(item.label + " (Comparator)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon("#compare_" + comparecounter));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("type", "checkbox");
              box3.attr("name", "required");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("name", "weight");
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("value", "1");
              box2.append(box5);

              $.each(item.parameters, function (j, parameter)
              {
			          var box4 = $(document.createElement('br'));
                box2.append(box4);

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");;
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description));

              box1.append(box2);

              // jsPlumb.addEndpoint('compare_1', endpointOptions);
              return box1;
            }
          });
          box.appendTo("#comparatorbox");
          list_item_id = list_item_id + 1;
        });

        var list_item_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #1484d4;");
        box.addClass("boxheaders");
        box.html("Aggregators").appendTo("#operators");
        box.appendTo("#operators");

        var box = $(document.createElement('div'));
        box.attr("id", "aggregatorbox");
        box.addClass("scrollboxes");
        box.appendTo("#operators");

        var sourcepaths = data.aggregators;
        $.each(sourcepaths, function (i, item)
        {
          aggregators[item.id] = new Object();
          aggregators[item.id]["name"] = item.label;
          aggregators[item.id]["description"] = item.description;
          aggregators[item.id]["parameters"] = item.parameters;

          var box = $(document.createElement('div'));
          box.addClass('draggable aggregators');
          box.attr("title", item.description);
          box.attr("id", "aggregator" + list_item_id);
          box.html("<span></span><small>" + item.label + "</small><p>" + item.label + "</p>");

          box.draggable(
          {
            helper: function ()
            {
              var box1 = $(document.createElement('div'));
              box1.addClass('dragDiv aggregateDiv');
              box1.attr("id", "aggregate_" + aggregatecounter);

              var box2 = $(document.createElement('small'));
              box2.addClass('name');
              var mytext = document.createTextNode(item.id);
              box2.append(mytext);
              box1.append(box2);
              var box2 = $(document.createElement('small'));
              box2.addClass('type');
              var mytext = document.createTextNode("Aggregate");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');

              var span = $(document.createElement('div'));
              span.attr("style", "width: 170px; white-space:nowrap; overflow:hidden; float: left;");
              span.attr("title", item.label + " (Aggregator)")
              var mytext = document.createTextNode(item.label + " (Aggregator)");
              span.append(mytext);
              box2.append(span);

              box2.append(getDeleteIcon("#aggregate_" + aggregatecounter));

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("name", "required");
              box3.attr("type", "checkbox");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("name", "weight");
              box5.attr("value", "1");
              box2.append(box5);

              $.each(item.parameters, function (j, parameter)
              {
                var box4 = $(document.createElement('br'));
                box2.append(box4);

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("name", parameter.name);
                box5.attr("type", "text");
                box5.attr("size", "10");;
                box2.append(box5);
              });

              box2.append(getHelpIcon(item.description));

              box1.append(box2);
			  
              return box1;
            }

          });
          box.appendTo("#aggregatorbox");
		  
          list_item_id = list_item_id + 1;
        });
        load();
      }
    },
    error: function (XMLHttpRequest, textStatus, errorThrown)
    {
      alert("Error: " + textStatus + " " + errorThrown);
    }
  });
}