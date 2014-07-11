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

enableTransformTasks = true;
enableVoidSourceButton = false;

var ws = {};
ws.activeProjectId ="";
ws.activeTaskId = "";
ws.activeNodesId = new Array();

// -- init
$(document).ready(
  function(){
    updateWorkspace();
  }
);

function updateActiveProject(projectId) {
  if(projectId != ws.activeProjectId) {
    ws.activeProjectId = projectId;
    //setCurrentProject(projectId);
  }
}

// -- active/unfold nodes handling functions
function saveOpenNodes(activeProject){
    ws.activeNodesId.length=0;
    if (activeProject!=="") ws.activeNodesId.push('project_'+activeProject);
    $('.collapsable').each(function(){if(this.getAttribute('id')) ws.activeNodesId.push(this.getAttribute('id'));});
  }

function loadOpenNodes(){
 for (var key in ws.activeNodesId)
  {
    if (document.getElementById(ws.activeNodesId[key]))
        document.getElementById(ws.activeNodesId[key]).setAttribute('class', 'collapsable');
    else ws.activeNodesId.splice(key,1);
  }
  if (ws.activeProjectId){
      if (document.getElementById(ws.activeProjectId))
          document.getElementById(ws.activeProjectId).setAttribute('class', 'collapsable');
      else ws.activeProjectId = '';
  }
  if (ws.activeTaskId) {
      if (document.getElementById(ws.activeTaskId))
          document.getElementById(ws.activeTaskId).setAttribute('class', 'collapsable active');
      else ws.activeTaskId = '';
  }
}

// -- display functions
function addLeaf(leaf, parent, desc)
{
          if (leaf){
             var leaf_ul = document.createElement("ul");
                 $(parent).append(leaf_ul);
             var leaf_li = document.createElement("li");
                 $(leaf_ul).append(leaf_li);
             var leaf_span = document.createElement("span");
                 $(leaf_span).addClass('file')
                 .text(desc + leaf);
                 $(leaf_li).append(leaf_span);
          }
}

function addAction(type, label, desc, action, parent, activeProject, enabled)
{
    var icon = getIcon(type);
    var action_button = document.createElement("button");
    if (enabled){
            $(action_button).click(function() {
                saveOpenNodes(activeProject);
                eval(action);
            })
            .addClass("action")
            .button({
                icons: {
                    primary: icon
                },
                label: label
            })

    } else {
            $(action_button).addClass('ui-icon ui-state-disabled '+icon);
    }
    $(action_button).attr("title",desc);
    $(parent).append(action_button);
}

function addDataSource(jsonDataSource,projectNode,projectName)
{
    var ds_ul = document.createElement("ul");
        $(projectNode).append(ds_ul);
    var ds_li = document.createElement("li");
        $(ds_li).attr("id",'datasource_'+projectName+'_'+jsonDataSource.name)
        .addClass('closed');
        $(ds_ul).append(ds_li);
    var ds_span = document.createElement("span");
        $(ds_span).addClass('source');
        $(ds_li).append(ds_span);

    var ds_label= document.createElement("span");
        $(ds_label).addClass('label')
        .text(jsonDataSource.name);
        $(ds_span).append(ds_label);

    var ds_actions = document.createElement("div");
        $(ds_actions).addClass('actions');
        $(ds_span).append(ds_actions);
        addAction('ds_edit', 'Edit', "Edit data source","editSource('"+projectName+"','"+ jsonDataSource.name+"')",ds_actions,projectName,true);
        addAction('delete','Remove',"Remove DataSource "+jsonDataSource.name,"confirmDelete('removeSource','"+projectName+"','"+jsonDataSource.name+"')",ds_actions,projectName,true);

    for(var p in jsonDataSource.params) {
      var param = jsonDataSource.params[p];
      addLeaf(param.value,ds_li, param.key + ': ');
    }
}

function addTransformTask(jsonTransformTask,projectNode,projectName)
{
  var lt_ul = document.createElement("ul");
  $(projectNode).append(lt_ul);
  var lt_li = document.createElement("li");
  $(lt_li).attr("id",'transformtask_'+projectName+'_'+jsonTransformTask.name)
      .addClass('closed');
  $(lt_ul).append(lt_li);
  var lt_span = document.createElement("span");
  $(lt_span).addClass('transform');
  $(lt_li).append(lt_span);

  var lt_label = document.createElement("span");
  $(lt_label).addClass('label')
      .text(jsonTransformTask.name);
  $(lt_span).append(lt_label);

  var lt_actions = document.createElement("div");
  $(lt_actions).addClass('actions');
  $(lt_span).append(lt_actions);
  addAction('link_edit', 'Metadata', "Edit metadata","editTransformTask('"+projectName+"','"+ jsonTransformTask.name+"')",lt_actions,projectName,true);
  addAction('link_spec', 'Open', "Edit transform task","openTransformTask('"+projectName+"','"+ jsonTransformTask.name+"')",lt_actions,projectName,true);
  addAction('delete', 'Remove',"Remove task","confirmDelete('removeTransformTask','"+projectName+"','"+ jsonTransformTask.name+"')",lt_actions,projectName,true);

  addLeaf(jsonTransformTask.source,lt_li, 'source: ');
  addLeaf(jsonTransformTask.dataset,lt_li, 'dataset: ');
}

function addLinkingTask(jsonLinkingTask,projectNode,projectName)
{
    var lt_ul = document.createElement("ul");
        $(projectNode).append(lt_ul);
    var lt_li = document.createElement("li");
        $(lt_li).attr("id",'linkingtask_'+projectName+'_'+jsonLinkingTask.name)
        .addClass('closed');
        $(lt_ul).append(lt_li);
    var lt_span = document.createElement("span");
        $(lt_span).addClass('link');
        $(lt_li).append(lt_span);

    var lt_label = document.createElement("span");
        $(lt_label).addClass('label')
        .text(jsonLinkingTask.name);
        $(lt_span).append(lt_label);

    var lt_actions = document.createElement("div");
        $(lt_actions).addClass('actions');
        $(lt_span).append(lt_actions);
    addAction('link_edit', 'Metadata', "Edit metadata","editLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName,true);
    addAction('link_spec', 'Open', "Edit linking task","openLinkingTask('"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName,true);
    addAction('delete', 'Remove',"Remove task","confirmDelete('removeLinkingTask','"+projectName+"','"+ jsonLinkingTask.name+"')",lt_actions,projectName,true);

    addLeaf(jsonLinkingTask.source,lt_li, 'source: ');
    addLeaf(jsonLinkingTask.target,lt_li, 'target: ');
    addLeaf(jsonLinkingTask.sourceDataset,lt_li, 'source dataset: ');
    addLeaf(jsonLinkingTask.targetDataset,lt_li, 'target dataset: ');
    addLeaf(jsonLinkingTask.linkType,lt_li, 'link type: ');
}

function addOutput(jsonOutput,projectNode,projectName)
{
    var ds_ul = document.createElement("ul");
        $(projectNode).append(ds_ul);
    var ds_li = document.createElement("li");
        $(ds_li).attr("id",'output_'+projectName+'_'+jsonOutput.name)
        .addClass('closed');
        $(ds_ul).append(ds_li);
    var ds_span = document.createElement("span");
        $(ds_span).addClass('output');
        $(ds_li).append(ds_span);

    var ds_label= document.createElement("span");
        $(ds_label).addClass('label')
        .text(jsonOutput.name);
        $(ds_span).append(ds_label);

    var ds_actions = document.createElement("div");
        $(ds_actions).addClass('actions');
        $(ds_span).append(ds_actions);
        addAction('ds_edit', 'Edit', "Edit output","editOutput('"+projectName+"','"+ jsonOutput.name+"')",ds_actions,projectName,true);
        addAction('delete','Remove',"Remove Output "+jsonOutput.name,"confirmDelete('removeOutput','"+projectName+"','"+jsonOutput.name+"')",ds_actions,projectName,true);

    for(var p in jsonOutput.params) {
      var param = jsonOutput.params[p];
      addLeaf(param.value,ds_li, param.key + ': ');
    }
}

function updateWorkspace(){
  $.get(baseUrl + '/api/workspace', function(data) {
    loadWorkspace(data);
  }).fail(function(request) { alert(request.responseText);  })
}

// -- display the workspace as treeview
function loadWorkspace(obj){
        removeNodeById("div_tree");

        // root folder
        if (!document.getElementById("root-folder")){
            var rootFolder = document.createElement("div");
            $(rootFolder).attr("id",'root-folder');
            $("#content").append(rootFolder);
        }

        var proj_actions = document.createElement("div");
            $(proj_actions).addClass('actions');
            $("#content").append(proj_actions);

        // new project button
        if (!document.getElementById("newproject")) {
            var newProj = document.createElement("div");
            $(newProj).attr("id",'newproject');
            addAction('add', 'Project','Create new project',"newProject()",newProj,"",true);
            $(proj_actions).append(newProj);
        }

        // import project button
        if (!document.getElementById("import")) {
            var importProj = document.createElement("div");
            $(importProj).attr("id",'import');
            addAction('import','Import','Import a project',"importProject()",importProj,"",true);
            $(proj_actions).append(importProj);
         }

        var tree = document.createElement("div");
            tree.id = "div_tree";

        var root = document.createElement("ul");
            $(root).attr("id", 'tree')
            .addClass('filetree');
            $(tree).append(root);

        // for each project
        for (var p in obj.workspace.project) {
            var project = obj.workspace.project[p];
            var  proj = document.createElement("li");
                $(proj).addClass('closed')
                .attr("id",'project_'+project.name)
                .attr("onclick", "updateActiveProject('" + project.name + "')");
                $(root).append(proj);
                var proj_span = document.createElement("span");
                    $(proj_span).addClass('folder');
                    $(proj).append(proj_span);

                var proj_label = document.createElement("span");
                    $(proj_label).addClass('label')
                    .text(project.name);
                    $(proj_span).append(proj_label);

                var proj_actions = document.createElement("div");
                    $(proj_actions).addClass('actions');
                    $(proj_span).append(proj_actions);
                addAction('edit_prefixes', 'Prefixes','Edit Prefixes',"editPrefixes('"+project.name+"')",proj_actions,project.name,true);
                addAction('edit_resources', 'Resources','Manage Resources',"editResources('"+project.name+"')",proj_actions,project.name,true);
                addAction('ds_add', 'Source','Add data source',"newSource('"+project.name+"')",proj_actions,project.name,true);
                if (enableVoidSourceButton===true) {
                  addAction('ds_add', 'Source from VoID','Add data source from VoID',"createVoidSourceTask('"+project.name+"')",proj_actions,project.name,true);
                }
                if (enableTransformTasks===true) {
                  addAction('link_add', 'Transform','Add transformation task',"newTransformTask('"+project.name+"')",proj_actions,project.name,true);
                }
                addAction('link_add', 'Linking','Add linking task',"newLinkingTask('"+project.name+"')",proj_actions,project.name,true);
                addAction('output_add', 'Output','Add output',"newOutput('"+project.name+"')",proj_actions,project.name,true);
                addAction('add_linkspec', 'Link Spec', 'Add link specification', "importLinkSpec('"+project.name+"')",proj_actions,project.name,true);
                addAction('export', 'Export','Export Project '+project.name,"exportProject('"+project.name+"')",proj_actions,project.name,true);
                addAction('delete', 'Remove','Remove project',"confirmDelete('deleteProject','"+project.name+"','')",proj_actions,'',true);


             // display dataSource
            for (var d in obj.workspace.project[p].dataSource)
            {
                addDataSource(project.dataSource[d],proj,project.name);
            }

            // display transformTask
            for (var l in obj.workspace.project[p].transformTask)
            {
              addTransformTask(project.transformTask[l],proj,project.name);
            }

            // display linkingTask
            for (var l in obj.workspace.project[p].linkingTask)
            {
                addLinkingTask(project.linkingTask[l],proj,project.name);
            }

            // display output
            for (var l in obj.workspace.project[p].output)
            {
                addOutput(project.output[l],proj,project.name);
            }
        }

        $("#content").append(tree);

        // uncollapse active project/task
        if(obj.workspace.activeProject){
             ws.activeProjectId = "project_"+obj.workspace.activeProject;
        }
        if(obj.workspace.activeTask)
            {
             var idPrefix;
             if(obj.workspace.activeTaskType === "LinkingTask") {
               idPrefix = 'linkingtask_';
             } else if(obj.workspace.activeTaskType === "TransformTask") {
               idPrefix = 'transformtask_';
             } else if(obj.workspace.activeTaskType === "DataSource") {
               idPrefix = 'datasource_';
             } else {
               idPrefix = 'output_';
             }
             ws.activeTaskId = idPrefix+obj.workspace.activeProject+"_"+obj.workspace.activeTask;
            }
        if (ws.activeNodesId.length>0 || ws.activeTaskId || ws.activeProjectId)  loadOpenNodes();

        $("#tree").treeview({animated:"fast"});
        $(".ui-icon").removeClass('ui-icon');
        $(".ui-icon-closethick").addClass('ui-icon');
    }

// - dialogs

// init and display the proper delete confirm dialog
function confirmDelete(action,proj,res){
     var confirmDialog = document.createElement("div");
         $(confirmDialog).attr("title",'Delete')
         .attr("id",'dialog');
     var dialogText = document.createElement("p");
         $(dialogText).text("Delete resource: "+proj+" "+res);
         $(confirmDialog).append(dialogText);

     $("#content").append(confirmDialog);

     $("#dialog").dialog({width: 400,
         modal: true,
         resizable: false,
         buttons: {
         "Yes, delete it": function() {
            callAction(action,proj,res);
            $(this).dialog("close");
         },
         "Cancel": function() {$(this).dialog("close");}
        }
        });
}

// -- utils
function removeNodeById(nodeId)
{
   var node = document.getElementById(nodeId);
   if (node) node.parentNode.removeChild(node);
}

function getIcon(type){
    var icon;
    switch (type)
    {
        case 'add' : icon = "new-project-icon";  break;
        case 'edit_prefixes' : icon = "edit-prefixes-icon";  break;
        case 'edit_resources' : icon = "edit-icon";  break;
        case 'ds_add' : icon = "add-icon";  break;
        case 'ds_edit' : icon = "edit-icon";  break;
        case 'link_add' : icon = "add-icon";  break;
        case 'link_edit' : icon = "edit-icon";  break;
        case 'link_spec': icon = "open-icon"; break;
        case 'output_add' : icon = "add-icon";  break;
        case 'delete' : icon = "remove-icon";  break;
        case 'import': icon = "import-icon"; break;
        case 'export': icon = "export-icon"; break;
        case 'add_linkspec' : icon = "add-ls-icon";  break;
    }
    return icon;
}

function callAction(action,proj,res){
    // work-around : passing the action as string parameter -> the action would be invoked anyway
    switch (action)
        {
        case 'deleteProject' :  deleteProject(proj);  break;
        case 'removeSource' :  removeSource(proj,res);  break;
        case 'removeTransformTask' : removeTransformTask(proj,res); break;
        case 'removeLinkingTask' : removeLinkingTask(proj,res); break;
        case 'removeOutput' : removeOutput(proj,res); break;
        default : alert("Error: Action \'"+action+"\' not defined!");
        }
}

function newProject() {
  showDialog(baseUrl + '/workspace/dialogs/newproject');
}

function importProject() {
  showDialog(baseUrl + '/workspace/dialogs/importproject');
}

function importLinkSpec(project) {
  showDialog(baseUrl + '/workspace/dialogs/importlinkspec/' + project);
}

function exportProject(project) {
  window.location = baseUrl + '/api/workspace/' + project + '/export'
}

function deleteProject(project) {
  deleteTask(baseUrl + '/api/workspace/' + project);
}

function editPrefixes(project) {
  showDialog(baseUrl + '/workspace/dialogs/prefixes/' + project);
}

function editResources(project) {
  showDialog(baseUrl + '/workspace/dialogs/resources/' + project);
}

function newSource(project) {
  showDialog(baseUrl + '/workspace/dialogs/newSource/' + project);
}

function editSource(project, source) {
  showDialog(baseUrl + '/workspace/dialogs/editSource/' + project + '/' + source);
}

function removeSource(project, source) {
  deleteTask(baseUrl + '/api/workspace/' + project + '/source/' + source);
}

function newTransformTask(project) {
  showDialog(baseUrl + '/workspace/dialogs/newTransformTask/' + project);
}

function editTransformTask(project, task) {
  showDialog(baseUrl + '/workspace/dialogs/editTransformTask/' + project + '/' + task);
}

function removeTransformTask(project, task) {
  deleteTask(baseUrl + '/transform/tasks/' + project + '/' + task);
}

function openTransformTask(project, task) {
  window.location = baseUrl + '/transform/' + project + '/' +  task + '/editor';
}

function newLinkingTask(project) {
  showDialog(baseUrl + '/workspace/dialogs/newLinkingTask/' + project);
}

function editLinkingTask(project, task) {
  showDialog(baseUrl + '/workspace/dialogs/editLinkingTask/' + project + '/' + task);
}

function removeLinkingTask(project, task) {
  deleteTask(baseUrl + '/linking/tasks/' + project + '/' + task);
}

function openLinkingTask(project, task) {
  window.location = baseUrl + '/linking/' + project + '/' + task + '/editor'
}

function newOutput(project) {
  showDialog(baseUrl + '/workspace/dialogs/newOutput/' + project);
}

function editOutput(project, output) {
  showDialog(baseUrl + '/workspace/dialogs/editOutput/' + project + '/' + output);
}

function removeOutput(project, output) {
  deleteTask(baseUrl + '/api/workspace/' + project + '/output/' + output);
}

function putTask(path, xml) {
  $.ajax({
    type: 'PUT',
    url: path,
    contentType: 'text/xml',
    processData: false,
    data: xml,
    success: function(data) {
      $('.dialog').dialog('close');
      updateWorkspace();
    },
    error: function(request) {
      alert(request.responseText);
    }
  });
}

function deleteTask(path) {
  $.ajax({
    type: 'DELETE',
    url: path,
    success: function(data) {
      updateWorkspace();
    },
    error: function(request) {
      alert(request.responseText);
    }
  });
}