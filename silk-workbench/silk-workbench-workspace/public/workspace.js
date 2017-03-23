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

// -- init
$(document).ready(
  function(){
    reloadWorkspace();
  }
);

function newProject() {
  showDialog(baseUrl + '/workspace/dialogs/newproject');
}

function importProject() {
  showDialog(baseUrl + '/workspace/dialogs/importproject');
}

function cloneProject(project) {
  showDialog(baseUrl + '/workspace/dialogs/cloneProject?project=' + project);
}

function cloneTask(project, task) {
  showDialog(baseUrl + '/workspace/dialogs/cloneTask?project=' + project + '&task=' + task);
}

function importLinkSpec(project) {
  showDialog(baseUrl + '/workspace/dialogs/importlinkspec/' + project);
}

function exportProject(project) {
  window.location = baseUrl + '/workspace/projects/' + project + '/export'
}

function executeProject(project) {
  showDialog(baseUrl + '/workspace/dialogs/executeProject/' + project);
}

function editPrefixes(project) {
  showDialog(baseUrl + '/workspace/dialogs/prefixes/' + project);
}

function editResources(project) {
  showDialog(baseUrl + '/workspace/dialogs/resources/' + project);
}

function reloadWorkspace() {
  $.get(baseUrl + "/workspace/tree", function(data) {
    $('#workspace_tree').html(data);
  }).fail(function(request) {
    alert("Error reloading workspace: " + request.responseText);
  });
}

function reloadWorkspaceInBackend() {
  $.post(baseUrl + '/workspace/reload', function(data) {
    reloadWorkspace();
  }).fail(function(request) {
    alert("Error reloading workspace: " + request.responseText);
  });
}

function workspaceDialog(relativePath) {
  showDialog(baseUrl + '/' + relativePath);
}

function putTask(path, xml) {
  $.ajax({
    type: 'PUT',
    url: path,
    contentType: 'text/xml',
    processData: false,
    data: xml,
    error: function(request) {
      alert(request.responseText);
    },
    success: function(request) {
      reloadWorkspace();
    }
  });
}

function deleteProject(project, task) {
  $.ajax({
    type: 'DELETE',
    url: baseUrl + '/workspace/projects/' + project,
    success: function(data) {
      reloadWorkspace();
    },
    error: function(request) {
      alert("Error deleting:" + request.responseText);
    }
  });
}

function deleteTask(project, task) {
  $.ajax({
    type: 'DELETE',
    url: baseUrl + '/workspace/projects/' + project + '/tasks/' + task,
    success: function(data) {
      reloadWorkspace();
    },
    error: function(request) {
      alert("Error deleting:" + request.responseText);
    }
  });
}

function deleteProjectConfirm(project, task) {
  showDialog(baseUrl + '/workspace/dialogs/removeproject/' + project);
}

function deleteTaskConfirm(project, task) {
  showDialog(baseUrl + '/workspace/dialogs/removetask/' + project + '/' + task);
}

function deleteResourceConfirm(name, path) {
  showDialog(baseUrl + '/workspace/dialogs/removeresource/' + name + "?path=" + encodeURIComponent(path), "secondary");
}