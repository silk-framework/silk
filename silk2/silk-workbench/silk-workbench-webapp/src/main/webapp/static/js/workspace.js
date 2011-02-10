

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

function addAction(name, action, parent)
{
         var action_span = document.createElement("span");
             action_span.setAttribute('class','action');
             action_span.setAttribute('onclick',action);
             action_span.innerHTML = name;
             parent.appendChild(action_span);
}

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

    // TODO - missing back-end function
    addAction('edit',"editDataSource('"+projectName+"','"+ jsonDataSource.name+"')",ds_name_li);
    // TODO - missing back-end function
    addAction('remove',"removeNodeById('datasource_"+projectName+"_"+jsonDataSource.name+"')",ds_name_li);

    addLeaf(jsonDataSource.url,ds_name_li, 'url: ');
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

    // TODO - missing back-end function
    addAction('edit restriction',"editLinkngRestriction('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_name_li);
    // TODO - missing back-end function
    addAction('edit link',"openLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_name_li);
    addAction('remove',"removeLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_name_li);
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

        var tree = document.createElement("div");
                tree.id = "div_tree";

        var root = document.createElement("ul");       function addLeaf(leaf, parent, desc)
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
}                                           var ds_name_li =

function addAction(name, action, parent)
{
         var action_span = document.createElement("span");
             action_span.setAttribute('class','action');
             action_span.setAttribute('onclick',action);
             action_span.innerHTML = name;
             parent.appendChild(action_span);
}
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

                addAction('add DataSource',"newDataSource('','"+project.name+"')",proj);
                addAction('add LinkingTask',"createLinkingTask('"+project.name+"')",proj);

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

        $("#tree").treeview();          
    }