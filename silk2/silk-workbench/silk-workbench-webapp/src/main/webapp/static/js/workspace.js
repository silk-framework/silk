
var activeNodesId = new Array();

function saveOpenNodes(activeProject){
    activeNodesId.length=0;
    if (activeProject!="") activeNodesId.push('project_'+activeProject);
    $('.collapsable').each(function(){if(this.getAttribute('id')) activeNodesId.push(this.getAttribute('id'));});
  }

function loadOpenNodes(){
 for (var key in activeNodesId)
  {
    if (document.getElementById(activeNodesId[key])) document.getElementById(activeNodesId[key]).setAttribute('class', 'collapsable');
  }
}

// -- callback functions --
function removeNodeById(nodeId)
{
   var node = document.getElementById(nodeId);
   if (node) node.parentNode.removeChild(node);
}

// - utils
function getIcon(type){
    var icon;
    switch (type)
    {
        case 'add' : icon = "ui-icon-circle-plus";  break;
        case 'ds_add' : icon = "ui-icon-cart";  break;
        case 'ds_edit' : icon = "ui-icon-wrench";  break;
        case 'link_add' : icon = "ui-icon-link";  break;
        case 'link_edit' : icon = "ui-icon-wrench";  break;
        case 'link_spec': icon = "ui-icon-shuffle"; break;
        case 'delete' : icon = "ui-icon-trash";  break;
        case 'import': icon = "ui-icon-arrowthickstop-1-s"; break;
        case 'export': icon = "ui-icon-arrowthick-1-ne"; break;
    }
    return icon;
}

// -- display functions --
function addLeaf(leaf, parent, desc)
{
          if (leaf){
             var leaf_ul = document.createElement("ul");
                 parent.appendChild(leaf_ul);
             var leaf_li = document.createElement("li");
                 leaf_ul.appendChild(leaf_li);
             var leaf_span = document.createElement("span");
                 leaf_span.setAttribute('class', 'file');
                 leaf_span.innerHTML = desc+leaf;
                 leaf_li.appendChild(leaf_span);
          }
}
// using jquery.ui icons
function addAction(type, desc, action, parent, activeProject)
{
        var icon = getIcon(type);
        var action_span = document.createElement("span");
             action_span.setAttribute('class','ui-icon '+icon );
             action_span.setAttribute('onclick', 'saveOpenNodes(\''+activeProject+'\');'+action);
             action_span.setAttribute('title', desc);
             parent.appendChild(action_span);
}
/* // using imgs as icons
function addAction(type, desc, action, parent, projectName)
{
        var action_img = document.createElement("img");
             action_img.setAttribute('class','icon');
             if (projectName!="") action_img.setAttribute('onclick', 'activeProject=\'project_'+projectName+'\';'+action);
             else action_img.setAttribute('onclick', action);   
             action_img.setAttribute('title', desc);
             action_img.setAttribute('src', 'static/img/icons/'+type+'.png');
             parent.appendChild(action_img);
}    */

function addDataSource(jsonDataSource,projectNode,projectName)
{
    var ds_name_ul = document.createElement("ul");
        projectNode.appendChild(ds_name_ul);
    var ds_name_li = document.createElement("li");
        ds_name_li.setAttribute('id','datasource_'+projectName+'_'+jsonDataSource.name);
        ds_name_li.setAttribute('class', 'closed');
        ds_name_ul.appendChild(ds_name_li);
    var ds_name_span = document.createElement("span");
        ds_name_span.setAttribute('class', 'folder');
        ds_name_span.innerHTML = 'Data Source: '+jsonDataSource.name;
        ds_name_li.appendChild(ds_name_span);

    var ds_actions = document.createElement("div");
        ds_actions.setAttribute('class', 'actions');
        ds_name_span.appendChild(ds_actions);
    addAction('ds_edit', "Edit DataSource "+jsonDataSource.name,"editSourceTask('"+projectName+"','"+ jsonDataSource.name+"')",ds_actions,projectName);
    addAction('delete',"Remove DataSource "+jsonDataSource.name,"confirmDelete('removeSourceTask','"+projectName+"','"+jsonDataSource.name+"')",ds_actions,projectName);

    for(var p in jsonDataSource.params) {
      var param = jsonDataSource.params[p];
      addLeaf(param.value,ds_name_li, param.key + ': ');
    }
}

function addLinkingTask(jsonLinkingTask,projectNode,projectName)
{
    var lt_name_ul = document.createElement("ul");
        projectNode.appendChild(lt_name_ul);
    var lt_name_li = document.createElement("li");
        lt_name_li.setAttribute('id','linkingtask_'+projectName+'_'+jsonLinkingTask.name);
        lt_name_li.setAttribute('class', 'closed');
        lt_name_ul.appendChild(lt_name_li);
    var lt_name_span = document.createElement("span");
        lt_name_span.setAttribute('class', 'folder');
        lt_name_span.innerHTML = 'Linking Task: '+jsonLinkingTask.name;
        lt_name_li.appendChild(lt_name_span);

    var lt_actions = document.createElement("div");
        lt_actions.setAttribute('class', 'actions');
        lt_name_span.appendChild(lt_actions);
    addAction('link_edit',"Edit "+jsonLinkingTask.name,"editLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName);
    addAction('link_spec',"Edit Link Specification "+jsonLinkingTask.name,"openLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName);
    addAction('delete',"Remove LinkingTask "+jsonLinkingTask.name,"confirmDelete('removeLinkingTask','"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName);

    addLeaf(jsonLinkingTask.source,lt_name_li, 'source: ');
    addLeaf(jsonLinkingTask.target,lt_name_li, 'target: ');
    addLeaf(jsonLinkingTask.sourceDataset,lt_name_li, 'source dataset: ');
    addLeaf(jsonLinkingTask.targetDataset,lt_name_li, 'target dataset: ');
}

// -- display the workspace as treeview
function updateWorkspace(obj){
        //var obj = jQuery.parseJSON('{"workspace": {   "project": [ {"name": "Project_1", "dataSource":[ {"name":"KEGG","url":"http://kegg.us"},{"name":"Wiki","url":"http://aba.com"}  ],  "linkingTask":[ {"name":"Gene","source":"KEGG","target":"Wiki","targetDataset":"rdf:type Wiki:Gene","sourceDataset":"rdf:type KEGG:gene"},{"name":"linkTask2","source":"KEGG","target":"Wiki"} ] }, {"name": "Project_2", "dataSource":[ {"name":"ABA","url":"http://aba.com"} ] }     ] } }');

        // TODO remove if using callback functions
        removeNodeById("div_tree");
        removeNodeById("newproject");

        // new project button
        var newProj = document.createElement("div");
            newProj.setAttribute('id','newproject');
            addAction('add','Create new Project',"createProject()",newProj,"");
            document.getElementById("content").appendChild(newProj);
    
        var tree = document.createElement("div");
                tree.id = "div_tree";

        var root = document.createElement("ul");      
            root.id = 'tree';
            root.setAttribute('class','filetree');
            tree.appendChild(root);

        // for each project                                  
        for (var p in obj.workspace.project) {
            var project = obj.workspace.project[p];
            var  proj = document.createElement("li");
                proj.setAttribute('class', 'closed');
                proj.setAttribute('id','project_'+project.name);
                root.appendChild(proj);
                var proj_span = document.createElement("span");
                    proj_span.setAttribute('class', 'folder');
                    proj_span.innerHTML = project.name;
                    proj.appendChild(proj_span);

                var proj_actions = document.createElement("div");
                    proj_actions.setAttribute('class', 'actions');
                    proj_span.appendChild(proj_actions);
                addAction('ds_add','Add DataSource',"createSourceTask('"+project.name+"')",proj_actions,project.name);
                addAction('link_add','Add LinkingTask',"createLinkingTask('"+project.name+"')",proj_actions,project.name);
                addAction('import','Import Project','',proj_actions,project.name);
                addAction('export','Export Project '+project.name,'',proj_actions,project.name);
                addAction('delete','Remove Project '+project.name,"confirmDelete('removeProject','"+project.name+"','')",proj_actions,'');


             // display dataSource
            for (var d in obj.workspace.project[p].dataSource)
            {
                addDataSource(project.dataSource[d],proj,project.name);
            }

            // display linkingTask
            for (var l in obj.workspace.project[p].linkingTask)
            {
                addLinkingTask(project.linkingTask[l],proj,project.name);
            }
        }

        document.getElementById("content").appendChild(tree);
        // unfold active project
        //if (activeProject!="")  document.getElementById(activeProject).setAttribute('class','collapsable');
        if (activeNodesId.length>0)  loadOpenNodes();

        $("#tree").treeview();
    }

// - dialogs
function callAction(action,proj,res){
    // (ugly) work-around : passing the action as parameter -> the action would be invoked anyway 
    switch (action)
        {
        case 'removeProject' :  removeProject(proj);  break;
        case 'removeSourceTask' :  removeSourceTask(proj,res);  break;
        case 'removeLinkingTask' : removeLinkingTask(proj,res); break;
        default : alert("Error: Action \'"+action+"\' not defined!");
        }
}

function confirmDelete(action,proj,res){
     var confirmDialog = document.createElement("div");
         confirmDialog.setAttribute('title','Delete');
         confirmDialog.setAttribute('id','dialog');
     var dialogText = document.createElement("p");
         dialogText.innerHTML = "Delete resource: "+proj+" "+res;
         confirmDialog.appendChild(dialogText);

     document.getElementById("content").appendChild(confirmDialog);

     $("#dialog").dialog({width: 400,
         modal: true,
         resizable: false,
         buttons: {
         "Yes, delete it": function() {callAction(action,proj,res); $(this).dialog("close");},
         "Cancel": function() {$(this).dialog("close");}
        }
        });
}
