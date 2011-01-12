var max_paths = 10;
var aggregatecounter = 0;
var transformcounter = 0;
var comparecounter = 0;
var sourcecounter = 0;
var targetcounter = 0;

var endpointOptions = {
    endpoint: new jsPlumb.Endpoints.Dot({radius:5}),
    isSource:true,
    style:{fillStyle:'#890685' },
    connectorStyle : { gradient:{stops:[[0,'#890685'], [1, '#359ace']] }, strokeStyle:'#890685', lineWidth:5 },
    isTarget:false,
    anchor: "RightMiddle"
};

var endpointOptions1 = {
    endpoint: new jsPlumb.Endpoints.Dot({radius:5}),
    isSource:false,
    style:{fillStyle:'#359ace'},
    connectorStyle : { gradient:{stops:[[0,'#359ace'], [1, '#35ceb7']] }, strokeStyle:'#359ace', lineWidth:5 },
    isTarget:true,
    maxConnections:4,
    anchor: "LeftMiddle"
};

var endpointOptions2 = {
    endpoint: new jsPlumb.Endpoints.Dot({radius:5}),
    isSource:true,
    style:{fillStyle:'#35ceb7'},
    connectorStyle : { gradient:{stops:[[0,'#35ceb7'], [1, '#359ace']] }, strokeStyle:'#359ace', lineWidth:5 },
    isTarget:false,
    maxConnections:4,
    anchor: "RightMiddle"
};

document.onselectstart = function () { return false; };

function parseXML(xml, level, level_y, last_element) {
    $(xml).find("> Aggregate").each(function() {
        var box1 = $(document.createElement('div'));
			box1.addClass('dragDiv aggregateDiv');
			box1.attr("id", "aggregate_" + aggregatecounter);
			var height = aggregatecounter * 100 + 150;
			var left =  1300-((level+1)*250);
			box1.attr("style", "left: "+left+"px; top: "+height+"px;");

			var number = "#aggregate_" + aggregatecounter;
			box1.draggable({containment: '.droppable'});
			box1.appendTo("#droppable_p");

            var box2 = $(document.createElement('h5'));
			box2.addClass('handler');
			var mytext = document.createTextNode("Aggregator: " + $(this).attr("type"));
			box2.append(mytext);
			box1.append(box2);

            var box2 = $(document.createElement('div'));
			box2.addClass('content');

			var mytext = document.createTextNode("required: ");
			box2.append(mytext);

			var box3 = $(document.createElement('input'));
            box3.attr("type", "checkbox");
            if ($(this).attr("required") == "true") {
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

			var endp_left = jsPlumb.addEndpoint('aggregate_'+aggregatecounter, endpointOptions1);
			var endp_right = jsPlumb.addEndpoint('aggregate_'+aggregatecounter, endpointOptions2);
			aggregatecounter = aggregatecounter + 1;
			if (last_element != "") {
                jsPlumb.connect({sourceEndpoint:last_element, targetEndpoint:endp_right});
 			}
			parseXML($(this), level + 1, 0, endp_left);

    });
    $(xml).find("> Compare").each(function()
    {
        var box1 = $(document.createElement('div'));
			box1.addClass('dragDiv compareDiv');
			box1.attr("id", "compare_"+comparecounter);
			var height = comparecounter*100+150;
            var left =  1300-((level+1)*250);
       		box1.attr("style", "left: "+left+"px; top: "+height+"px;");
			var number = "#compare_"+comparecounter;
			box1.draggable({containment: '.droppable'});
			box1.appendTo("#droppable_p");

			var box2 = $(document.createElement('h5'));
			box2.addClass('handler');
			var mytext = document.createTextNode("Comparator: " + $(this).attr("metric"));
			box2.append(mytext);
			box1.append(box2);

            var box2 = $(document.createElement('div'));
			box2.addClass('content');

			var mytext = document.createTextNode("required: ");
			box2.append(mytext);

			var box3 = $(document.createElement('input'));
            box3.attr("type", "checkbox");
            if ($(this).attr("required") == "true") {
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

			var endp_left = jsPlumb.addEndpoint('compare_'+comparecounter, endpointOptions1);
			var endp_right = jsPlumb.addEndpoint('compare_'+comparecounter, endpointOptions2);
			comparecounter = comparecounter + 1;
			if (last_element != "") {
                jsPlumb.connect({sourceEndpoint:last_element, targetEndpoint:endp_right});
 			}
			parseXML($(this), level + 1, 0, endp_left);
    });

    $(xml).find("> TransformInput").each(function() {

			var box1 = $(document.createElement('div'));
			box1.addClass('dragDiv transformDiv');
			box1.attr("id", "transform_"+transformcounter);

            var height = transformcounter * 100 + 150;
            var left =  1300-((level+1)*250);
            box1.attr("style", "left: "+left+"px; top: "+height+"px;");

			//box1.html("<h5 class='handler'>Transformation: " + json.TransformInput[transformcounter].transformfunction + "</h5><div class='content'>required: <input type='checkbox' checked><br/>weight: <input type='text' size='2' value='1' /></div>");
			var number = "#transform_"+transformcounter;
			box1.draggable({containment: '.droppable'});
			box1.appendTo("#droppable_p");

			var box2 = $(document.createElement('h5'));
			box2.addClass('handler');
			var mytext = document.createTextNode("Transformation: " + $(this).attr("transformfunction"));
			box2.append(mytext);
			box1.append(box2);

            var box2 = $(document.createElement('div'));
			box2.addClass('content');

			box1.append(box2);

			var endp_left = jsPlumb.addEndpoint('transform_'+transformcounter, endpointOptions1);
			var endp_right = jsPlumb.addEndpoint('transform_'+transformcounter, endpointOptions2);
			transformcounter = transformcounter + 1;
			if (last_element != "") {
                jsPlumb.connect({sourceEndpoint:last_element, targetEndpoint:endp_right});
 			}
			parseXML($(this), level + 1, 0, endp_left);
	});
    $(xml).find("> Input").each(function() {

			var box1 = $(document.createElement('div'));
			box1.addClass('dragDiv sourcePath');
			box1.attr("id", "source_" + sourcecounter);

            var height = sourcecounter * 100 + 150;
            var left = 1300-((level+1)*250);
            box1.attr("style", "left: "+left+"px; top: "+height+"px;");

			//box1.html("<h5 class='handler'>" + json.Input[sourcecounter].path + "</h5><div class='content'></div>");
			var number = "#source_"+sourcecounter;
			box1.draggable({containment: '.droppable'});
			box1.appendTo("#droppable_p");

			var box2 = $(document.createElement('h5'));
			box2.addClass('handler');
			var mytext = document.createTextNode($(this).attr("path"));
			box2.append(mytext);
			box1.append(box2);

            var box2 = $(document.createElement('div'));
			box2.addClass('content');

			box1.append(box2);

			var endp_right = jsPlumb.addEndpoint('source_'+sourcecounter, endpointOptions);
			sourcecounter = sourcecounter + 1;
			if (last_element != "") {
                jsPlumb.connect({sourceEndpoint:last_element, targetEndpoint:endp_right});
 			}
			parseXML($(this), level + 1, 0, endp_right);
	});
}

function load() {
    // alert(linkCondition);
    $(linkSpec).find("> LinkCondition").each(function() {
        parseXML($(this), 0, 0, "");
    });
}


function getHTML (who,deep) {
    if(!who || !who.tagName) return '';
    var txt, el= document.createElement("div");
    el.appendChild(who.cloneNode(deep));
    txt= el.innerHTML;
    el= null;
    return txt;
}

function save() {
	//alert (JSON.stringify(c));
}


$(function() {
    $("#droppable").droppable({
        drop: function(ev, ui) {
            //draggedNumber = ui.helper.attr('id').search(/([0-9])/);
            $(this).append($(ui.helper).clone());
            if (ui.helper.attr('id').search(/aggregate/) != -1){
                jsPlumb.addEndpoint('aggregate_'+aggregatecounter, endpointOptions1);
                jsPlumb.addEndpoint('aggregate_'+aggregatecounter, endpointOptions2);
                var number = "#aggregate_"+aggregatecounter;
                $(number).draggable({containment: '.droppable'});
                aggregatecounter = aggregatecounter + 1;
            }
            if (ui.helper.attr('id').search(/transform/) != -1){
                jsPlumb.addEndpoint('transform_'+transformcounter, endpointOptions1);
                jsPlumb.addEndpoint('transform_'+transformcounter, endpointOptions2);
                var number = "#transform_"+transformcounter;
                $(number).draggable({containment: '.droppable'});
                transformcounter = transformcounter + 1;
            }
            if (ui.helper.attr('id').search(/compare/) != -1){
                jsPlumb.addEndpoint('compare_'+comparecounter, endpointOptions1);
                jsPlumb.addEndpoint('compare_'+comparecounter, endpointOptions2);
                var number = "#compare_"+comparecounter;
                $(number).draggable({containment: '.droppable'});
                comparecounter = comparecounter + 1;
            }
            if (ui.helper.attr('id').search(/source/) != -1){
                jsPlumb.addEndpoint('source_'+sourcecounter, endpointOptions);
                var number = "#source_"+sourcecounter;
                $(number).draggable({containment: '.droppable'});
                sourcecounter = sourcecounter + 1;
            }
            if (ui.helper.attr('id').search(/target/) != -1){
                jsPlumb.addEndpoint('target_'+targetcounter, endpointOptions);
                var number = "#target_"+targetcounter;
                $(number).draggable({containment: '.droppable'});
                targetcounter = targetcounter + 1;
            }
        }
    });
});

function getPropertyPaths() {
        $.getJSON("http://160.45.137.90:30300/api/project/paths?max=10",
            function(data) {

            var global_id = 0;

                var box = $(document.createElement('div'));
                box.html("<span style='font-weight: bold;'>Source:</span> " + data.source.id).appendTo("#paths");
                box.appendTo("#paths");

                var box = $(document.createElement('div'));
                box.addClass('more');
                box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.source.restriction).appendTo("#paths");
                box.appendTo("#paths");

                var sourcepaths = data.source.paths;
                $.each(sourcepaths, function(i, item){
                    var box = $(document.createElement('div'));
                    box.addClass('draggable');
                    box.attr("id", "source"+global_id);
                    box.html("<span></span><small>"+item.path+"</small><p>"+item.path+"</p>");
                    box.draggable({
                        helper: function() {
                          var box1 = $(document.createElement('div'));
                          box1.addClass('dragDiv sourcePath');
                          box1.attr("id", "source_"+sourcecounter);
                          box1.html("<h5 class='handler'>"+item.path+"</h5><div class='content'></div>");
                          return box1;
                        }
                    });
                    box.appendTo("#paths");

                    //jsPlumb.addEndpoint('source'+global_id, endpointOptions);

                    global_id = global_id + 1;

                });

                var availablePaths = data.source.availablePaths;
                if (max_paths < availablePaths) {
                    var box = $(document.createElement('div'));
                    box.html('<a href="" class="more">&darr; more source paths...</a>').appendTo("#paths");
                    box.appendTo("#paths");
                }

                var box = $(document.createElement('div'));
                box.html("<span style='font-weight: bold;'>Target:</span> " + data.target.id).appendTo("#paths");
                box.appendTo("#paths");

                var box = $(document.createElement('div'));
                box.addClass('more');
                box.html("<span style='font-weight: bold;'>Restriction:</span> " + data.target.restriction).appendTo("#paths");
                box.appendTo("#paths");

                var global_id = 0;

                var sourcepaths = data.target.paths;
                $.each(sourcepaths, function(i, item){
                    var box = $(document.createElement('div'));
                    box.addClass('draggable');
                    box.attr("id", "target"+global_id);
                    box.html("<span></span><small>"+item.path+"</small><p>"+item.path+"</p>");
                    box.draggable({
                        helper: function() {
                          var box1 = $(document.createElement('div'));
                          box1.addClass('dragDiv targetPath');
                          box1.attr("id", "target_"+targetcounter);
                          box1.html("<h5 class='handler'>"+item.path+"</h5><div class='content'></div>");
                          return box1;
                        }
                    });
                    box.appendTo("#paths");

                    // jsPlumb.addEndpoint('target'+global_id, endpointOptions);

                    global_id = global_id + 1;

                });

                var availablePaths = data.target.availablePaths;
                if (max_paths < availablePaths) {
                    var box = $(document.createElement('div'));
                    box.html('<a href="" class="more">&darr; more target paths... ('+availablePaths+' in total)</a>').appendTo("#paths");
                    box.appendTo("#paths");

                }


                // document.getElementById("paths").appendChild(box);

                // $("<div/>").attr("", item.media.m).appendTo("#images");
                // if ( i == 3 ) return false;






            }
        );
}


function getOperators() {
    $.getJSON("http://160.45.137.90:30300/api/project/operators",
      function(data) {

        var global_id = 0;

        var box = $(document.createElement('div'));
        box.attr("style", "color: #0cc481; font-weight: bold;");
        box.html("Transformations").appendTo("#operators");
        box.appendTo("#operators");

        var sourcepaths = data.transformations;
        $.each(sourcepaths, function(i, item){
            var box = $(document.createElement('div'));
            box.addClass('draggable tranformations');
            box.attr("id", "transformation"+global_id);
            box.attr("title", item.description);
            box.html("<span></span><small>"+item.label+"</small><p>"+item.label+"</p>");
            box.draggable({
                helper: function() {
                  var box1 = $(document.createElement('div'));
                  box1.addClass('dragDiv transformDiv');
                  box1.attr("id", "transform_"+transformcounter);
                  box1.html("<h5 class='handler'>Transformation: "+item.label+"</h5><div class='content'></div>");
                  // jsPlumb.addEndpoint('transform_1', endpointOptions);
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
        $.each(sourcepaths, function(i, item){
            var box = $(document.createElement('div'));
            box.addClass('draggable comparators');
            box.attr("id", "comparator"+global_id);
            box.attr("title", item.description);
            box.html("<span></span><small>"+item.label+"</small><p>"+item.label+"</p>");
            box.draggable({
                helper: function() {
                  var box1 = $(document.createElement('div'));
                  box1.addClass('dragDiv compareDiv');
                  box1.attr("id", "compare_"+comparecounter);
                  box1.html("<h5 class='handler'>Comparator: "+item.label+"</h5><div class='content'>required: <input type='checkbox'/><br/>weight: <input type='text' size='2' value='1' /></div>");

                  $.each(item.parameters, function(j, parameter) {
                    // alert("param " + parameter.name);
                  });
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
        $.each(sourcepaths, function(i, item){
            var box = $(document.createElement('div'));
            box.addClass('draggable aggregators');
            box.attr("title", item.description);
            box.attr("id", "aggregator"+global_id);
            box.html("<span></span><small>"+item.label+"</small><p>"+item.label+"</p>");

            box.draggable({
                helper: function() {
                  var box1 = $(document.createElement('div'));
                  box1.addClass('dragDiv aggregateDiv');
                  box1.attr("id", "aggregate_"+aggregatecounter);

                  box1.html("<h5 class='handler'>Aggregator: "+item.label+"</h5><div class='content'>required: <input type='checkbox'/><br/>weight: <input type='text' size='2' value='1' /></div>");

                  $.each(item.parameters, function(j, parameter) {
                    alert("param " + parameter.name);
                  });

                  // jsPlumb.addEndpoint('aggregate_1', endpointOptions);
                  return box1;
                }

            });
            box.appendTo("#operators");
            // jsPlumb.addEndpoint('aggregator'+global_id, endpointOptions1);
            // jsPlumb.addEndpoint('aggregator'+global_id, endpointOptions2);
            global_id = global_id + 1;
        });

        }
    );
}