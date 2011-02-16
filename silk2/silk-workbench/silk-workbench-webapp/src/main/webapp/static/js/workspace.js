
var activeProject = "";


// -- callback functions --
function removeNodeById(nodeId)
{
   var node = document.getElementById(nodeId);
   if (node) node.parentNode.removeChild(node);
}

// TODO - this implementation would close/refresh the tree anyway -> therefore could be better to update workspaceVar and invoke UpdateWorkspace()
function newDataSource(jsonDataSource,projectName)
{
    var projectNode =  document.getElementById("project_"+projectName);
    if (projectNode)
    {
        //addDataSource(jsonDataSource, projectNode, projectName )
        addDataSource(jQuery.parseJSON('{"name":"KEGG","url":"http://kegg.us"}'), projectNode, projectName );
    }
}

// - utils
function getIcon(type){
    if (type=='add') return "ui-icon-circle-plus";
    else if (type=='ds_add') return "ui-icon-cart";
    else if (type=='ds_edit') return "ui-icon-wrench";  
    else if (type=='link_add') return "ui-icon-link";
    else if (type=='link_edit') return "ui-icon-wrench";
    else if (type=='delete') return "ui-icon-trash";
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
function addAction(type, desc, action, parent, projectName)
{
        var icon = getIcon(type);
        var action_span = document.createElement("span");
             action_span.setAttribute('class','ui-icon '+icon );
             if (projectName!="") action_span.setAttribute('onclick', 'activeProject=\'project_'+projectName+'\';'+action);
             else action_span.setAttribute('onclick', action);
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
        ds_name_ul.setAttribute('id','datasource_'+projectName+'_'+jsonDataSource.name);
        projectNode.appendChild(ds_name_ul);
    var ds_name_li = document.createElement("li");
        ds_name_li.setAttribute('class', 'closed');
        ds_name_ul.appendChild(ds_name_li);
    var ds_name_span = document.createElement("span");
        ds_name_span.setAttribute('class', 'folder');
        ds_name_span.innerHTML = 'Data Source: '+jsonDataSource.name;
        ds_name_li.appendChild(ds_name_span);

    addAction('ds_edit', "Edit DataSource "+jsonDataSource.name,"editSourceTask('"+projectName+"','"+ jsonDataSource.name+"')",ds_name_li,projectName);
   // addAction('del',"Remove DataSource "+jsonDataSource.name,"confirmDelete(removeSourceTask('"+projectName+"','"+ jsonDataSource.name+"'))",ds_name_li,projectName);
   addAction('delete',"Remove DataSource "+jsonDataSource.name,"confirmDelete('removeSourceTask','"+projectName+"','"+jsonDataSource.name+"')",ds_name_li,projectName);

    // TODO - missing back-end function
    //addAction('remove',"removeNodeById('datasource_"+projectName+"_"+jsonDataSource.name+"')",ds_name_li);

    for(var p in jsonDataSource.params) {
      var param = jsonDataSource.params[p];
      addLeaf(param.value,ds_name_li, param.key + ': ');
    }
}

function addLinkingTask(jsonLinkingTask,projectNode,projectName)
{
    var lt_name_ul = document.createElement("ul");
        // TODO id must be unique
        lt_name_ul.setAttribute('id','linkingtask_'+projectName+'_'+jsonLinkingTask.name);
        projectNode.appendChild(lt_name_ul);
    var lt_name_li = document.createElement("li");
        lt_name_li.setAttribute('class', 'closed');
        lt_name_ul.appendChild(lt_name_li);
    var lt_name_span = document.createElement("span");
        lt_name_span.setAttribute('class', 'folder');
        lt_name_span.innerHTML = 'Linking Task: '+jsonLinkingTask.name;
        lt_name_li.appendChild(lt_name_span);

    addAction('link_edit',"Edit LinkingTask "+jsonLinkingTask.name,"openLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_name_li,projectName);
    addAction('delete',"Remove LinkingTask "+jsonLinkingTask.name,"confirmDelete('removeLinkingTask','"+projectName+"','"+ jsonLinkingTask.name+"')",lt_name_li,projectName);
    // TODO using callback functions would be..
    //addAction('remove',"removeLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"',removeNodeById(linkingtask_"+projectName+"_"+jsonLinkingTask.name+")",lt_name_li);

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

                addAction('ds_add','Add DataSource',"createSourceTask('"+project.name+"')",proj,project.name);
                addAction('link_add','Add LinkingTask',"createLinkingTask('"+project.name+"')",proj,project.name);
                addAction('delete','Remove Project '+project.name,"confirmDelete('removeProject','"+project.name+"','')",proj,"");

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
        if (activeProject!="")  document.getElementById(activeProject).setAttribute('class','collapsable');

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
