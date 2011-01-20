var max_paths = 10;
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
  isTarget: true,
  anchor: "RightMiddle"
};

var endpointOptions1 =
{
  endpoint: new jsPlumb.Endpoints.Dot(
  {
    radius: 5
  }),
  isSource: true,
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
  maxConnections: 4,
  anchor: "RightMiddle"
};

document.onselectstart = function ()
{
  return false;
};


function parseXML(xml, level, level_y, last_element)
{
  $(xml).find("> Aggregate").each(function ()
  {
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv aggregateDiv');
    box1.attr("id", "aggregate_" + aggregatecounter);
    var height = aggregatecounter * 100 + 150;
    var left = 1300 - ((level + 1) * 250);
    box1.attr("style", "left: " + left + "px; top: " + height + "px;");

    var number = "#aggregate_" + aggregatecounter;
    box1.draggable(
    {
      containment: '.droppable'
    });
    box1.appendTo("#droppable_p");

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
    var mytext = document.createTextNode("Aggregator: " + aggregators[$(this).attr("type")]);
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
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
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions2);
    aggregatecounter = aggregatecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: last_element,
        targetEndpoint: endp_right
      });
    }
    parseXML($(this), level + 1, 0, endp_left);

  });
  $(xml).find("> Compare").each(function ()
  {
    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv compareDiv');
    box1.attr("id", "compare_" + comparecounter);
    var height = comparecounter * 100 + 150;
    var left = 1300 - ((level + 1) * 250);
    box1.attr("style", "left: " + left + "px; top: " + height + "px;");
    var number = "#compare_" + comparecounter;
    box1.draggable(
    {
      containment: '.droppable'
    });
    box1.appendTo("#droppable_p");

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
    var mytext = document.createTextNode("Comparator: " + comparators[$(this).attr("metric")]);
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    var mytext = document.createTextNode("required: ");
    box2.append(mytext);

    var box3 = $(document.createElement('input'));
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
    box5.attr("size", "2");
    box5.attr("value", $(this).attr("weight"));
    box2.append(box5);

    //alert(comparators[$(this).attr("metric")]["parameters"]);
/*
			$.each(comparators[$(this).attr("metric")].parameters, function(j, parameter) {
				var box4 = $(document.createElement('br'));
				box2.append(box4);

				var mytext = document.createTextNode(parameter.name + ": ");
				box2.append(mytext);

				var box5 = $(document.createElement('input'));
				box5.attr("type", "text");
				box5.attr("size", "10");;
				box2.append(box5);
			});
			*/

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('compare_' + comparecounter, endpointOptions2);
    comparecounter = comparecounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: last_element,
        targetEndpoint: endp_right
      });
    }
    parseXML($(this), level + 1, 0, endp_left);
  });

  $(xml).find("> TransformInput").each(function ()
  {

    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv transformDiv');
    box1.attr("id", "transform_" + transformcounter);

    var height = transformcounter * 100 + 150;
    var left = 1300 - ((level + 1) * 250);
    box1.attr("style", "left: " + left + "px; top: " + height + "px;");

    var number = "#transform_" + transformcounter;
    box1.draggable(
    {
      containment: '.droppable'
    });
    box1.appendTo("#droppable_p");

	var box2 = $(document.createElement('small'));
	box2.addClass('name');
	var mytext = document.createTextNode($(this).attr("transformfunction"));
	box2.append(mytext);
	box1.append(box2);
	var box2 = $(document.createElement('small'));
	box2.addClass('type');
	var mytext = document.createTextNode("TransformInput");
	box2.append(mytext);
	box1.append(box2);
	
    var box2 = $(document.createElement('h5'));
    box2.addClass('handler');
    var mytext = document.createTextNode("Transformation: " + transformations[$(this).attr("transformfunction")]);
    box2.append(mytext);
    box1.append(box2);

    var box2 = $(document.createElement('div'));
    box2.addClass('content');

    box1.append(box2);

    var endp_left = jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions1);
    var endp_right = jsPlumb.addEndpoint('transform_' + transformcounter, endpointOptions2);
    transformcounter = transformcounter + 1;
    if (last_element != "")
    {
      jsPlumb.connect(
      {
        sourceEndpoint: last_element,
        targetEndpoint: endp_right
      });
    }
    parseXML($(this), level + 1, 0, endp_left);
  });
  $(xml).find("> Input").each(function ()
  {

    var box1 = $(document.createElement('div'));
    box1.addClass('dragDiv sourcePath');
    box1.attr("id", "source_" + sourcecounter);

    var height = sourcecounter * 100 + 150;
    var left = 1300 - ((level + 1) * 250);
    box1.attr("style", "left: " + left + "px; top: " + height + "px;");

    //box1.html("<h5 class='handler'>" + json.Input[sourcecounter].path + "</h5><div class='content'></div>");
    var number = "#source_" + sourcecounter;
    box1.draggable(
    {
      containment: '.droppable'
    });
    box1.appendTo("#droppable_p");

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
    var mytext = document.createTextNode($(this).attr("path"));
    box2.append(mytext);
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
        sourceEndpoint: last_element,
        targetEndpoint: endp_right
      });
    }
    parseXML($(this), level + 1, 0, endp_right);
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
    parseXML($(this), 0, 0, "");
  });
  $(linkSpec).find("> LinkType").each(function ()
  {
    $("#linktype").attr("value", $(this).text());
  });
  $(linkSpec).find("> Filter").each(function ()
  {
    $("#linklimit").attr("value", $(this).attr("limit"));
    $("#threshold").attr("value", $(this).attr("threshold"));
  });
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
		xml.setAttribute("path", elName);
	} else if (elType == "TransformInput") {
		xml.setAttribute("function", elName);
	} else if (elType == "Aggregate") {
		xml.setAttribute("type", elName);
	} else if (elType == "Compare") {
		xml.setAttribute("metric", elName);
	}
	
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
	return xml;
}

function serializeLinkSpec() {
  //alert (JSON.stringify(c));
  
  var c = jsPlumb.getConnections();
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
  if (root != null)
  {
    // alert(connections + "\n" + root);
    var xml = document.createElement("Interlink");
	xml.setAttribute("id", interlinkId);
	
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
	linkcondition.appendChild(createNewElement(root));
	xml.appendChild(linkcondition);

	var linktype = document.createElement("LinkType");
	var linktypeText = document.createTextNode($("#linktype").val());
	linktype.appendChild(linktypeText);
	xml.appendChild(linktype);

	var filter = document.createElement("Filter");
	filter.setAttribute("limit", $("#linklimit").val());
	filter.setAttribute("threshold", $("#threshold").val());
	xml.appendChild(filter);

	var outputs = document.createElement("Outputs");
	xml.appendChild(outputs);

	var xmlString = getHTML(xml, true);
	xmlString = xmlString.replace('xmlns="http://www.w3.org/1999/xhtml"', "");
	// alert(xmlString);
	return xmlString;
  }
  else
  {
    alert("No tree root found!");
    return "Error";
  }
}

$(function ()
{
  $("#droppable").droppable(
  {
    drop: function (ev, ui)
    {
      //draggedNumber = ui.helper.attr('id').search(/([0-9])/);
      $(this).append($(ui.helper).clone());
      if (ui.helper.attr('id').search(/aggregate/) != -1)
      {
        jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions1);
        jsPlumb.addEndpoint('aggregate_' + aggregatecounter, endpointOptions2);
        var number = "#aggregate_" + aggregatecounter;
        $(number).draggable(
        {
          containment: '.droppable'
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
          containment: '.droppable'
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
          containment: '.droppable'
        });
        comparecounter = comparecounter + 1;
      }
      if (ui.helper.attr('id').search(/source/) != -1)
      {
        jsPlumb.addEndpoint('source_' + sourcecounter, endpointOptions);
        var number = "#source_" + sourcecounter;
        $(number).draggable(
        {
          containment: '.droppable'
        });
        sourcecounter = sourcecounter + 1;
      }
      if (ui.helper.attr('id').search(/target/) != -1)
      {
        jsPlumb.addEndpoint('target_' + targetcounter, endpointOptions);
        var number = "#target_" + targetcounter;
        $(number).draggable(
        {
          containment: '.droppable'
        });
        targetcounter = targetcounter + 1;
      }
    }
  });
});

function encodeHtml(value)
{
  encodedHtml = value.replace("<", "&lt;");
  encodedHtml = encodedHtml.replace(">", "&gt;");
  encodedHtml = encodedHtml.replace("\"", '\\"');
  return encodedHtml;
}

function getPropertyPaths()
{

  var url = "/api/project/paths?max=10";
  $.getJSON(url, function (data)
  {
    if(data.isLoading) {
      setTimeout("getPropertyPaths();", 1000);
    }
    else
    {
    document.getElementById("paths").removeChild(document.getElementById("loading"));


    var global_id = 0;

    var box = $(document.createElement('div'));
    box.html("<span style='font-weight: bold;'>Source:</span> " + data.source.id).appendTo("#paths");
    box.appendTo("#paths");

    var box = $(document.createElement('div'));
    box.addClass('more');
    box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.source.restrictions).appendTo("#paths");
    box.appendTo("#paths");

    var sourcepaths = data.source.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable');
      box.attr("id", "source" + global_id);
      box.html("<span></span><p>" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv sourcePath');
          box1.attr("id", "source_" + sourcecounter);
          box1.html("<small class=\"name\">" + encodeHtml(item.path) + "</small><small class=\"type\">Input</small><h5 class='handler'>" + encodeHtml(item.path) + "</h5><div class='content'></div>");
          return box1;
        }
      });
      box.appendTo("#paths");

      //jsPlumb.addEndpoint('source'+global_id, endpointOptions);
      global_id = global_id + 1;

    });

    var box = $(document.createElement('div'));
    box.addClass('draggable');
    box.attr("id", "source" + global_id);
    box.html("<span> </span><small> </small><p> </p>");
    box.draggable(
    {
      helper: function ()
      {
        var box1 = $(document.createElement('div'));
        box1.addClass('dragDiv sourcePath');
        box1.attr("id", "source_" + sourcecounter);
        box1.html("<small class=\"name\"></small><small class=\"type\">Input</small><h5 class='handler'><input type=\"text\" size=\"20\"/></h5><div class='content'></div>");
        return box1;
      }
    });
    box.appendTo("#paths");

    var availablePaths = data.source.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html('<a href="" class="more">&darr; more source paths...</a>').appendTo("#paths");
      box.appendTo("#paths");
    }

    var box = $(document.createElement('div'));
    box.html("<span style='font-weight: bold;'>Target:</span> " + data.target.id).appendTo("#paths");
    box.appendTo("#paths");

    var box = $(document.createElement('div'));
    box.addClass('more');
    box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.target.restrictions).appendTo("#paths");
    box.appendTo("#paths");

    var global_id = 0;

    var sourcepaths = data.target.paths;
    $.each(sourcepaths, function (i, item)
    {
      var box = $(document.createElement('div'));
      box.addClass('draggable');
      box.attr("id", "target" + global_id);
      box.html("<span></span><small>" + encodeHtml(item.path) + "</small><p>" + encodeHtml(item.path) + "</p>");
      box.draggable(
      {
        helper: function ()
        {
          var box1 = $(document.createElement('div'));
          box1.addClass('dragDiv targetPath');
          box1.attr("id", "target_" + targetcounter);
          box1.html("<small class=\"name\">" + encodeHtml(item.path) + "</small><small class=\"type\">Input</small><h5 class='handler'>" + encodeHtml(item.path) + "</h5><div class='content'></div>");
          return box1;
        }
      });
      box.appendTo("#paths");

      global_id = global_id + 1;

    });

    var box = $(document.createElement('div'));
    box.addClass('draggable');
    box.attr("id", "target" + global_id);
    box.html("<span> </span><small> </small><p> </p>");
    box.draggable(
    {
      helper: function ()
      {
        var box1 = $(document.createElement('div'));
        box1.addClass('dragDiv sourcePath');
        box1.attr("id", "source_" + sourcecounter);
        box1.html("<small class=\"name\"></small><small class=\"type\">Input</small><h5 class='handler'><input type=\"text\" size=\"20\"/></h5><div class='content'></div>");
        return box1;
      }
    });
    box.appendTo("#paths");

    var availablePaths = data.target.availablePaths;
    if (max_paths < availablePaths)
    {
      var box = $(document.createElement('div'));
      box.html('<a href="" class="more">&darr; more target paths... (' + availablePaths + ' in total)</a>');
      box.appendTo("#paths");

    }
    }
  });
}

function getOperators()
{

  $.ajax(
  {
    type: "GET",
    url: "/api/project/operators",
    contentType: "application/json; charset=utf-8",
    dataType: "json",
    timeout: 2000,
    success: function (data, textStatus, XMLHttpRequest)
    {
      // alert("success: " + data + " " + textStatus + " " + XMLHttpRequest.status);
      if (XMLHttpRequest.status >= 200 && XMLHttpRequest.status < 300)
      {
        var global_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #0cc481; font-weight: bold;");
        box.html("Transformations").appendTo("#operators");
        box.appendTo("#operators");

        var sourcepaths = data.transformations;
        $.each(sourcepaths, function (i, item)
        {
          transformations[item.id] = item.label;
          var box = $(document.createElement('div'));
          box.addClass('draggable tranformations');
          box.attr("id", "transformation" + global_id);
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
              var mytext = document.createTextNode("Transformation: " + item.label);
              box2.append(mytext);

              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              transformations[item.id].parameters = item.parameters;
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
                box5.attr("type", "text");
                box5.attr("size", "10");;
                box2.append(box5);
              });

              box1.append(box2);
              return box1;
            }
          });
          box.appendTo("#operators");


          // jsPlumb.addEndpoint('transformation'+global_id, endpointOptions1);
          // jsPlumb.addEndpoint('transformation'+global_id, endpointOptions2);
          global_id = global_id + 1;


        });

        var global_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #e59829; font-weight: bold;");
        box.html("Comparators").appendTo("#operators");
        box.appendTo("#operators");

        var sourcepaths = data.comparators;
        $.each(sourcepaths, function (i, item)
        {
          comparators[item.id] = item.label;
          comparators[item.id]["parameters"] = item.parameters;
          var box = $(document.createElement('div'));
          box.addClass('draggable comparators');
          box.attr("id", "comparator" + global_id);
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
              var mytext = document.createTextNode("Comparator: " + item.label);
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("type", "checkbox");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
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
                box5.attr("type", "text");
                box5.attr("size", "10");;
                box2.append(box5);
              });

              box1.append(box2);

              // jsPlumb.addEndpoint('compare_1', endpointOptions);
              return box1;
            }
          });
          box.appendTo("#operators");
          // jsPlumb.addEndpoint('comparator'+global_id, endpointOptions1);
          // jsPlumb.addEndpoint('comparator'+global_id, endpointOptions2);
          global_id = global_id + 1;
        });

        var global_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #1484d4; font-weight: bold;");
        box.html("Aggregators").appendTo("#operators");
        box.appendTo("#operators");

        var sourcepaths = data.aggregators;
        $.each(sourcepaths, function (i, item)
        {
          aggregators[item.id] = item.label;
          var box = $(document.createElement('div'));
          box.addClass('draggable aggregators');
          box.attr("title", item.description);
          box.attr("id", "aggregator" + global_id);
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
              var mytext = document.createTextNode("Compare");
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('h5'));
              box2.addClass('handler');
              var mytext = document.createTextNode("Aggregator: " + item.label);
              box2.append(mytext);
              box1.append(box2);

              var box2 = $(document.createElement('div'));
              box2.addClass('content');

              var mytext = document.createTextNode("required: ");
              box2.append(mytext);

              var box3 = $(document.createElement('input'));
              box3.attr("type", "checkbox");
              box2.append(box3);

              var box4 = $(document.createElement('br'));
              box2.append(box4);

              var mytext = document.createTextNode("weight: ");
              box2.append(mytext);

              var box5 = $(document.createElement('input'));
              box5.attr("type", "text");
              box5.attr("size", "2");
              box5.attr("value", "1");
              box2.append(box5);

              aggregators[item.id].parameters = item.parameters;
              $.each(item.parameters, function (j, parameter)
              {
                var box4 = $(document.createElement('br'));
                box2.append(box4);

                var mytext = document.createTextNode(parameter.name + ": ");
                box2.append(mytext);

                var box5 = $(document.createElement('input'));
                box5.attr("type", "text");
                box5.attr("size", "10");;
                box2.append(box5);
              });

              box1.append(box2);

              // jsPlumb.addEndpoint('aggregate_1', endpointOptions);
              return box1;
            }

          });
          box.appendTo("#operators");
          // jsPlumb.addEndpoint('aggregator'+global_id, endpointOptions1);
          // jsPlumb.addEndpoint('aggregator'+global_id, endpointOptions2);
          global_id = global_id + 1;
        });
        load();
      }
    },
    error: function (XMLHttpRequest, textStatus, errorThrown)
    {
      alert("error:" + textStatus + " " + errorThrown);
    }
  });
}